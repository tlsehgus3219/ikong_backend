package com.ikongserver.service;

import com.ikongserver.dto.EventDto.EmergencyAlertListResponse;
import com.ikongserver.dto.EventDto.EmergencyAlertResponse;
import com.ikongserver.dto.EventDto.EventSummaryResponse;
import com.ikongserver.dto.EventDto.ResponseEvent;
import com.ikongserver.dto.VitalDto.VitalRequestDto;
import com.ikongserver.entity.ConditionType;
import com.ikongserver.entity.Device;
import com.ikongserver.entity.EmergencyEvent;
import com.ikongserver.entity.Guardian;
import com.ikongserver.entity.Notification;
import com.ikongserver.entity.UserCondition;
import com.ikongserver.entity.UserGuardianMap;
import com.ikongserver.entity.Users;
import com.ikongserver.entity.Vital;
import com.ikongserver.repository.DeviceRepository;
import com.ikongserver.repository.EmergencyEventRepository;
import com.ikongserver.repository.GuardianRepository;
import com.ikongserver.repository.NotificationRepository;
import com.ikongserver.repository.UserConditionRepository;
import com.ikongserver.repository.UserGuardianMapRepository;
import com.ikongserver.repository.UsersRepository;
import com.ikongserver.repository.VitalRepository;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmergencyEventService {

    // 고령자 표준 임계값 — 개인 데이터 부족 시 fallback
    private static final int ELDERLY_HR_WARN_LOW = 60, ELDERLY_HR_WARN_HIGH = 100;
    private static final int ELDERLY_BR_WARN_LOW = 12, ELDERLY_BR_WARN_HIGH = 20;

    // 개인 기준선 임계 비율 — 가중평균 기준 ±15% 벗어나면 이상으로 판정
    private static final double WARNING_RATIO = 0.15;

    // 연속 이상 감지 기준 — 30초 연속 이상부터 알림
    private static final int ABNORMAL_CONSECUTIVE = 30;

    // 개인 기준선 계산 윈도우 — 최근 N일 데이터만 사용
    private static final int BASELINE_WINDOW_DAYS = 7;

    // 개인 기준선 가중치 반감기 — 1일
    private static final long BASELINE_HALF_LIFE_MS = 24 * 60 * 60 * 1000L;

    // 워밍업 데이터 수 — 사용자 최초 N건은 기준선 계산에서 제외
    private static final int BASELINE_WARMUP_COUNT = 10;

    // VITAL_ISSUE 쿨다운 — 동일 시리얼 번호당 3분
    private static final long VITAL_ANOMALY_COOLDOWN_MS = 3 * 60 * 1000L;

    // WARNING(HEART/BREATH_ISSUE) 쿨다운 — 에피소드 종료 후 5분 이내 재발 시 이벤트 생성 억제
    private static final long WARNING_COOLDOWN_MS = 5 * 60 * 1000L;

    // 데이터 공백 감지 — 10초 이상 데이터 없으면 연속 카운터 리셋
    private static final long DATA_GAP_RESET_MS = 10_000L;

    // 낙상 이벤트 쿨다운 — 5분
    private static final long FALL_COOLDOWN_MS = 5 * 60 * 1000L;

    // 심박/호흡 이상 재알림 간격 — 1분
    private static final long VITAL_RECALERT_MS = 60 * 1000L;

    // 캐시 TTL
    private static final long BASELINE_CACHE_TTL = 6 * 60 * 60 * 1000L;
    private static final long CONDITIONS_CACHE_TTL = 60 * 60 * 1000L;

    private final Map<String, Long> lastVitalAnomalyTimeMap = new ConcurrentHashMap<>();
    private final Map<Long, Long> lastWarningEventTimeMap = new ConcurrentHashMap<>();
    private final Map<Long, Long> lastFallEventTimeMap = new ConcurrentHashMap<>();
    private final Map<Long, Long> lastCheckTimeMap = new ConcurrentHashMap<>();

    private final Map<Long, int[]> baselineCache = new ConcurrentHashMap<>();
    private final Map<Long, Long> baselineCacheTime = new ConcurrentHashMap<>();
    private final Map<Long, List<ConditionType>> conditionsCache = new ConcurrentHashMap<>();
    private final Map<Long, Long> conditionsCacheTime = new ConcurrentHashMap<>();

    private final Map<Long, Integer> heartConsecutiveMap = new ConcurrentHashMap<>();
    private final Map<Long, Integer> breathConsecutiveMap = new ConcurrentHashMap<>();
    private final Map<Long, Episode> activeEpisodeMap = new ConcurrentHashMap<>();

    private static class Episode {
        final Long eventId;
        final String eventType;
        final String severity;
        final String title;
        final String message;
        long lastAlertTime;

        Episode(Long eventId, String eventType, String severity,
                String title, String message, long lastAlertTime) {
            this.eventId = eventId;
            this.eventType = eventType;
            this.severity = severity;
            this.title = title;
            this.message = message;
            this.lastAlertTime = lastAlertTime;
        }
    }

    private final EmergencyEventRepository emergencyEventRepository;
    private final UsersRepository userRepository;
    private final GuardianRepository guardianRepository;
    private final UserGuardianMapRepository userGuardianMapRepository;
    private final NotificationRepository notificationRepository;
    private final VitalRepository vitalRepository;
    private final DeviceRepository deviceRepository;
    private final UserConditionRepository userConditionRepository;
    private final FcmService fcmService;

    // 서버 재시작 후 DB의 PENDING 이벤트를 기반으로 활성 episode 복구
    // 연속 카운터는 복구 불가 — 재시작 후 30초 재관찰 필요
    @PostConstruct
    @Transactional
    public void restoreActiveEpisodes() {
        emergencyEventRepository.findByStatusOrderByCreatedAtDesc("PENDING")
            .forEach(event -> {
                Long userId = event.getUser().getId();
                String[] tm = buildTitleMessage(event.getUser(), event.getEventType());
                activeEpisodeMap.putIfAbsent(userId, new Episode(
                    event.getId(), event.getEventType(), event.getSeverity(),
                    tm[0], tm[1], System.currentTimeMillis()
                ));
            });
    }

    // 라즈베리파이 LCD [알림] 버튼 — 피보호자가 직접 도움 요청
    @Transactional
    public ResponseEvent createManualAlert(String serialNum) {
        Device device = deviceRepository.findBySerialNum(serialNum)
            .orElseThrow(() -> new IllegalArgumentException("기기를 찾을 수 없습니다."));
        Users user = device.getUser();

        EmergencyEvent event = EmergencyEvent.builder()
            .user(user).eventType("MANUAL_ALERT").status("PENDING")
            .severity("CRITICAL").device(device).build();
        emergencyEventRepository.save(event);

        String title = "도움 요청";
        String message = user.getName() + "님이 도움을 요청했습니다";
        createNotificationsAndPush(event, user, title, message);

        return new ResponseEvent(event.getId(), event.getEventType(), event.getStatus(), event.getCreatedAt());
    }

    // 낙상 감지 — 5분 쿨다운으로 중복 이벤트 방지
    @Transactional
    public boolean checkFallEvent(VitalRequestDto vitalDto, Users user, Device device) {
        if (!vitalDto.isFallDetected()) return false;

        Long userId = user.getId();
        long now = System.currentTimeMillis();
        Long lastFallTime = lastFallEventTimeMap.get(userId);

        if (lastFallTime != null && now - lastFallTime < FALL_COOLDOWN_MS) {
            return true;
        }

        lastFallEventTimeMap.put(userId, now);
        EmergencyEvent fallEvent = EmergencyEvent.builder()
            .user(user).eventType("FALL").status("PENDING")
            .severity("CRITICAL").device(device).build();
        emergencyEventRepository.save(fallEvent);

        String message = user.getName() + "님 낙상이 감지되었습니다. [심각] 즉시 확인이 필요합니다.";
        createNotificationsAndPush(fallEvent, user, "낙상 감지", message);
        return true;
    }

    // 심박수/호흡수 이상 감지
    @Transactional
    public boolean checkHeartBreathEvent(int heartRate, int breathRate, Users user, Device device) {
        Long userId = user.getId();
        long now = System.currentTimeMillis();

        // 데이터 공백 감지 — 10초 이상 공백이면 연속 카운터 리셋 (오알람 방지)
        Long lastCheck = lastCheckTimeMap.get(userId);
        if (lastCheck != null && now - lastCheck > DATA_GAP_RESET_MS) {
            heartConsecutiveMap.put(userId, 0);
            breathConsecutiveMap.put(userId, 0);
        }
        lastCheckTimeMap.put(userId, now);

        int[] baseline = getPersonalBaseline(user);
        List<ConditionType> conditions = getCachedConditions(user);

        int hrCount = isAbnormal(heartRate, baseline, true, conditions)
            ? heartConsecutiveMap.merge(userId, 1, Integer::sum)
            : resetAndGet(heartConsecutiveMap, userId);

        int brCount = isAbnormal(breathRate, baseline, false, conditions)
            ? breathConsecutiveMap.merge(userId, 1, Integer::sum)
            : resetAndGet(breathConsecutiveMap, userId);

        boolean hrBad = hrCount >= ABNORMAL_CONSECUTIVE;
        boolean brBad = brCount >= ABNORMAL_CONSECUTIVE;

        String serialNum = device.getSerialNum();
        if (hrBad && brBad) {
            return handleCondition(user, device, "CRITICAL", "VITAL_ISSUE", serialNum);
        } else if (hrBad) {
            return handleCondition(user, device, "WARNING", "HEART_ISSUE", serialNum);
        } else if (brBad) {
            return handleCondition(user, device, "WARNING", "BREATH_ISSUE", serialNum);
        }

        activeEpisodeMap.remove(userId);
        return false;
    }

    private int resetAndGet(Map<Long, Integer> map, Long key) {
        map.put(key, 0);
        return 0;
    }

    // 이상 상태 처리
    private boolean handleCondition(Users user, Device device, String severity, String eventType,
        String serialNum) {
        Long userId = user.getId();
        Episode active = activeEpisodeMap.get(userId);
        long now = System.currentTimeMillis();

        boolean isNew = (active == null);
        boolean isEscalation = active != null
            && "WARNING".equals(active.severity) && "CRITICAL".equals(severity);
        // CRITICAL→WARNING 완화
        boolean isDeescalation = active != null
            && "CRITICAL".equals(active.severity) && "WARNING".equals(severity);
        // 같은 심각도지만 이상 항목 전환 (HEART_ISSUE ↔ BREATH_ISSUE)
        boolean isTypeChange = active != null
            && !active.eventType.equals(eventType) && !isEscalation && !isDeescalation;

        if (isDeescalation || isTypeChange) {
            // 상태 변화 — episode 메시지 갱신 후 즉시 재알림
            String[] tm = buildTitleMessage(user, eventType);
            activeEpisodeMap.put(userId,
                new Episode(active.eventId, eventType, severity, tm[0], tm[1], now));
            reAlertUnreadGuardians(active.eventId, tm[0], tm[1]);
            return true;
        }

        if (isNew || isEscalation) {
            // 에스컬레이션 시 기존 WARNING 이벤트 RESOLVED 처리
            if (isEscalation) {
                emergencyEventRepository.findById(active.eventId)
                    .ifPresent(e -> e.updateStatus("RESOLVED"));
            }

            // VITAL_ISSUE 쿨다운 체크
            if ("VITAL_ISSUE".equals(eventType) && serialNum != null) {
                Long lastTime = lastVitalAnomalyTimeMap.get(serialNum);
                if (lastTime != null && now - lastTime < VITAL_ANOMALY_COOLDOWN_MS) {
                    return true;
                }
                lastVitalAnomalyTimeMap.put(serialNum, now);
            }

            // WARNING 이벤트 쿨다운 체크 (HEART/BREATH_ISSUE 반복 스팸 방지)
            if ("WARNING".equals(severity)) {
                Long lastTime = lastWarningEventTimeMap.get(userId);
                if (lastTime != null && now - lastTime < WARNING_COOLDOWN_MS) {
                    return true;
                }
                lastWarningEventTimeMap.put(userId, now);
            }

            EmergencyEvent event = EmergencyEvent.builder()
                .user(user).eventType(eventType).status("PENDING")
                .severity(severity).device(device).build();
            emergencyEventRepository.save(event);

            String[] tm = buildTitleMessage(user, eventType);
            createNotificationsAndPush(event, user, tm[0], tm[1]);
            activeEpisodeMap.put(userId,
                new Episode(event.getId(), eventType, severity, tm[0], tm[1], now));
            return true;
        }

        // 동일 상태 지속 — 1분마다 미확인 보호자에게만 재알림
        if (now - active.lastAlertTime >= VITAL_RECALERT_MS) {
            reAlertUnreadGuardians(active.eventId, active.title, active.message);
            active.lastAlertTime = now;
        }
        return true;
    }

    private String[] buildTitleMessage(Users user, String eventType) {
        String name = user.getName();
        return switch (eventType) {
            case "VITAL_ISSUE" -> new String[]{
                "[심각] 심박수·호흡수 이상",
                name + "님의 심박수와 호흡수에 동시에 이상이 감지되었습니다. 즉시 확인이 필요합니다."};
            case "HEART_ISSUE" -> new String[]{
                "[주의] 심박수 이상",
                name + "님의 심박수가 평소와 다르게 측정되고 있습니다. 상태를 주의깊게 살펴보세요."};
            case "BREATH_ISSUE" -> new String[]{
                "[주의] 호흡수 이상",
                name + "님의 호흡수가 평소와 다르게 측정되고 있습니다. 상태를 주의깊게 살펴보세요."};
            default -> new String[]{"바이탈 이상", name + "님의 바이탈에 이상이 감지되었습니다."};
        };
    }

    private void reAlertUnreadGuardians(Long eventId, String title, String message) {
        EmergencyEvent event = emergencyEventRepository.findById(eventId).orElse(null);
        if (event == null) return;

        userGuardianMapRepository.findByUser(event.getUser()).stream()
            .filter(m -> "Y".equals(m.getIsActive()))
            .map(UserGuardianMap::getGuardian)
            .forEach(guardian ->
                notificationRepository
                    .findFirstByEmergencyEventIdAndGuardianIdOrderBySentAtDesc(eventId, guardian.getId())
                    .filter(n -> "N".equals(n.getReadYN()))
                    .ifPresent(n -> fcmService.sendPushNotification(
                        guardian.getFcmToken(), title, message, "EMERGENCY")));
    }

    // 질환 목록 캐시 — 1시간 TTL (이전: 매초 DB 조회)
    private List<ConditionType> getCachedConditions(Users user) {
        Long userId = user.getId();
        Long lastCalc = conditionsCacheTime.get(userId);
        if (lastCalc != null
            && System.currentTimeMillis() - lastCalc < CONDITIONS_CACHE_TTL
            && conditionsCache.containsKey(userId)) {
            return conditionsCache.get(userId);
        }
        List<ConditionType> conditions = userConditionRepository.findByUser(user).stream()
            .map(UserCondition::getConditionType).toList();
        conditionsCache.put(userId, conditions);
        conditionsCacheTime.put(userId, System.currentTimeMillis());
        return conditions;
    }

    // 개인 기준선(지수 감쇠 가중평균) — 6시간 TTL 캐시
    private int[] getPersonalBaseline(Users user) {
        Long userId = user.getId();
        Long lastCalc = baselineCacheTime.get(userId);

        if (lastCalc != null
            && System.currentTimeMillis() - lastCalc < BASELINE_CACHE_TTL
            && baselineCache.containsKey(userId)) {
            return baselineCache.get(userId);
        }

        List<Vital> earliest = vitalRepository.findTop10ByUserOrderByRecordedAtAsc(user);
        if (earliest.size() < BASELINE_WARMUP_COUNT) return null;

        LocalDateTime warmupEnd = earliest.get(BASELINE_WARMUP_COUNT - 1).getRecordedAt();
        List<Vital> vitals = vitalRepository
            .findByUserAndRecordedAtAfter(user, LocalDateTime.now().minusDays(BASELINE_WINDOW_DAYS))
            .stream().filter(v -> v.getRecordedAt().isAfter(warmupEnd)).toList();

        if (vitals.isEmpty()) return null;

        LocalDateTime now = LocalDateTime.now();
        double weightSum = 0, hrWeightedSum = 0, brWeightedSum = 0;
        for (Vital v : vitals) {
            long ageMs = Duration.between(v.getRecordedAt(), now).toMillis();
            double weight = Math.pow(0.5, (double) ageMs / BASELINE_HALF_LIFE_MS);
            weightSum += weight;
            hrWeightedSum += v.getHeartRate() * weight;
            brWeightedSum += v.getBreathRate() * weight;
        }

        int[] baseline = {
            (int) Math.round(hrWeightedSum / weightSum),
            (int) Math.round(brWeightedSum / weightSum)
        };
        baselineCache.put(userId, baseline);
        baselineCacheTime.put(userId, System.currentTimeMillis());
        return baseline;
    }

    private boolean isAbnormal(int value, int[] baseline, boolean isHeart,
        List<ConditionType> conditions) {
        if (!conditions.isEmpty()) {
            return isAbnormalByCondition(value, baseline, isHeart, conditions);
        }
        if (baseline == null) {
            return isHeart
                ? (value < ELDERLY_HR_WARN_LOW || value > ELDERLY_HR_WARN_HIGH)
                : (value < ELDERLY_BR_WARN_LOW || value > ELDERLY_BR_WARN_HIGH);
        }
        int baselineValue = isHeart ? baseline[0] : baseline[1];
        return value < baselineValue * (1 - WARNING_RATIO)
            || value > baselineValue * (1 + WARNING_RATIO);
    }

    private boolean isAbnormalByCondition(int value, int[] baseline, boolean isHeart,
        List<ConditionType> conditions) {

        boolean exceedsConditionThreshold = checkConditionThreshold(value, baseline, isHeart, conditions);

        boolean exceedsBaseline = false;
        if (baseline != null) {
            int base = isHeart ? baseline[0] : baseline[1];
            exceedsBaseline = value < base * (1 - WARNING_RATIO) || value > base * (1 + WARNING_RATIO);
        }

        boolean hasImmediateDanger = conditions.stream().anyMatch(c -> !c.requireBoth);
        if (hasImmediateDanger) return exceedsConditionThreshold;

        return baseline == null ? exceedsConditionThreshold : exceedsConditionThreshold && exceedsBaseline;
    }

    private boolean checkConditionThreshold(int value, int[] baseline, boolean isHeart,
        List<ConditionType> conditions) {

        if (isHeart) {
            int warnHigh = conditions.stream()
                .filter(c -> c.hrWarnHigh != null).mapToInt(c -> c.hrWarnHigh)
                .min().orElse(ELDERLY_HR_WARN_HIGH);
            int warnLow = conditions.stream()
                .filter(c -> c.hrWarnLow != null).mapToInt(c -> c.hrWarnLow)
                .max().orElse(ELDERLY_HR_WARN_LOW);

            boolean copdAbnormal = conditions.contains(ConditionType.COPD)
                && baseline != null && value > baseline[0] * 1.15;

            return value >= warnHigh || value <= warnLow || copdAbnormal;
        } else {
            int warnHigh = conditions.stream()
                .filter(c -> c.brWarnHigh != null).mapToInt(c -> c.brWarnHigh)
                .min().orElse(ELDERLY_BR_WARN_HIGH);
            return value >= warnHigh;
        }
    }

    private void createNotificationsAndPush(EmergencyEvent event, Users user,
        String fcmTitle, String message) {
        userGuardianMapRepository.findByUser(user).stream()
            .filter(m -> "Y".equals(m.getIsActive()))
            .forEach(mapping -> {
                Guardian guardian = mapping.getGuardian();
                notificationRepository.save(Notification.builder()
                    .emergencyEvent(event).guardian(guardian)
                    .message(message).status("SUCCESS").build());
                fcmService.sendPushNotification(guardian.getFcmToken(), fcmTitle, message, "EMERGENCY");
            });
    }

    @Transactional(readOnly = true)
    public EventSummaryResponse getEventSummaryForGuardian(Long guardianId) {
        Guardian guardian = guardianRepository.findById(guardianId)
            .orElseThrow(() -> new IllegalArgumentException("보호자를 찾을 수 없습니다."));

        List<Users> users = userGuardianMapRepository.findByGuardian(guardian).stream()
            .map(UserGuardianMap::getUser).toList();

        if (users.isEmpty()) return new EventSummaryResponse(0, 0);

        long unresolvedCount = emergencyEventRepository.countByUserInAndStatus(users, "PENDING");
        long resolvedCount = emergencyEventRepository.countByUserInAndStatus(users, "RESOLVED");

        return new EventSummaryResponse(resolvedCount, unresolvedCount);
    }

    @Transactional(readOnly = true)
    public EmergencyAlertListResponse getEmergencyAlertsForGuardian(Long guardianId) {
        Guardian guardian = guardianRepository.findById(guardianId)
            .orElseThrow(() -> new IllegalArgumentException("보호자를 찾을 수 없습니다."));

        List<Users> users = userGuardianMapRepository.findByGuardian(guardian).stream()
            .map(UserGuardianMap::getUser).toList();

        if (users.isEmpty()) return new EmergencyAlertListResponse(List.of());

        List<EmergencyAlertResponse> alerts = emergencyEventRepository
            .findByUserInOrderByCreatedAtDesc(users).stream()
            .map(event -> new EmergencyAlertResponse(
                event.getId(), event.getUser().getName(), event.getEventType(),
                event.getStatus(), event.getCreatedAt(), event.getEventDescription()))
            .toList();

        return new EmergencyAlertListResponse(alerts);
    }

    @Transactional
    public ResponseEvent resolveEvent(Long guardianId, Long eventId) {
        Guardian guardian = guardianRepository.findById(guardianId)
            .orElseThrow(() -> new IllegalArgumentException("보호자를 찾을 수 없습니다."));

        EmergencyEvent event = emergencyEventRepository.findById(eventId)
            .orElseThrow(() -> new IllegalArgumentException("응급 이벤트를 찾을 수 없습니다."));

        boolean authorized = userGuardianMapRepository
            .findByGuardianAndIsActive(guardian, "Y").stream()
            .anyMatch(m -> m.getUser().getId().equals(event.getUser().getId()));

        if (!authorized) throw new IllegalStateException("해당 이벤트를 해결할 권한이 없습니다.");

        if (!"RESOLVED".equals(event.getStatus())) {
            event.updateStatus("RESOLVED");
            fcmService.sendPushNotification(
                guardian.getFcmToken(), "응급 상황 해결",
                event.getUser().getName() + "님의 응급 상황이 해결되었습니다.", "ALERT");
        }

        return new ResponseEvent(event.getId(), event.getEventType(), event.getStatus(), event.getCreatedAt());
    }

    // PENDING 이벤트가 실제로 있는 사용자에게만 FCM 발송
    @Transactional
    public void resolveAllEvents(Long guardianId) {
        Guardian guardian = guardianRepository.findById(guardianId)
            .orElseThrow(() -> new IllegalArgumentException("보호자를 찾을 수 없습니다."));

        List<Users> users = userGuardianMapRepository.findByGuardian(guardian).stream()
            .map(UserGuardianMap::getUser).toList();

        if (users.isEmpty()) return;

        List<EmergencyEvent> pendingEvents = emergencyEventRepository.findByUserInAndStatus(users, "PENDING");
        pendingEvents.forEach(event -> event.updateStatus("RESOLVED"));

        Set<Long> resolvedUserIds = pendingEvents.stream()
            .map(e -> e.getUser().getId()).collect(Collectors.toSet());

        users.stream()
            .filter(u -> resolvedUserIds.contains(u.getId()))
            .forEach(u -> fcmService.sendPushNotification(
                guardian.getFcmToken(), "응급 상황 해결",
                u.getName() + "님의 모든 응급 상황이 해결되었습니다.", "ALERT"));
    }

    @Transactional(readOnly = true)
    public ResponseEvent getLatestPendingEvent(long userId) {
        Users user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        return emergencyEventRepository.findTopByUserAndStatusOrderByCreatedAtDesc(user, "PENDING")
            .map(event -> new ResponseEvent(
                event.getId(), event.getEventType(), event.getStatus(), event.getCreatedAt()))
            .orElse(null);
    }
}
