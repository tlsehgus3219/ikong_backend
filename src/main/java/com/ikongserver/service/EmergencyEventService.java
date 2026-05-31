package com.ikongserver.service;

import com.ikongserver.dto.EventDto.EmergencyAlertListResponse;
import com.ikongserver.dto.EventDto.EmergencyAlertResponse;
import com.ikongserver.dto.EventDto.EventSummaryResponse;
import com.ikongserver.dto.EventDto.ResponseEvent;
import com.ikongserver.dto.VitalDto.VitalRequestDto;
import com.ikongserver.entity.Device;
import com.ikongserver.entity.EmergencyEvent;
import com.ikongserver.entity.Guardian;
import com.ikongserver.entity.Notification;
import com.ikongserver.entity.UserGuardianMap;
import com.ikongserver.entity.Users;
import com.ikongserver.entity.Vital;
import com.ikongserver.repository.DeviceRepository;
import com.ikongserver.repository.EmergencyEventRepository;
import com.ikongserver.repository.GuardianRepository;
import com.ikongserver.repository.NotificationRepository;
import com.ikongserver.repository.UserGuardianMapRepository;
import com.ikongserver.repository.UsersRepository;
import com.ikongserver.repository.VitalRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmergencyEventService {

    // 고령자 표준 임계값 — 개인 데이터 부족 시 fallback
    private static final int ELDERLY_HR_WARN_LOW = 60, ELDERLY_HR_WARN_HIGH = 100;
    private static final int ELDERLY_BR_WARN_LOW = 12, ELDERLY_BR_WARN_HIGH = 20;

    // 개인 기준선 임계 비율 — 중앙값 기준 ±15% 벗어나면 이상으로 판정
    private static final double WARNING_RATIO = 0.15;

    // 연속 이상 감지 기준 — 30초 연속 이상부터 알림 (매초 데이터 수신 기준)
    private static final int ABNORMAL_CONSECUTIVE = 30;

    // 개인 기준선 최소 데이터 수 — 3일치(25,920개) 이상이어야 신뢰 가능
    private static final long MIN_RECORDS_FOR_BASELINE = 25920L;

    // 개인 기준선 캐시 — 6시간마다 재계산 (매초 DB 조회 방지)
    private static final long BASELINE_CACHE_TTL = 6 * 60 * 60 * 1000L;
    private final Map<Long, int[]> baselineCache = new ConcurrentHashMap<>();
    private final Map<Long, Long> baselineCacheTime = new ConcurrentHashMap<>();

    // 연속 이상 카운터 — userId별 심박수/호흡수 연속 이상 횟수
    private final Map<Long, Integer> heartConsecutiveMap = new ConcurrentHashMap<>();
    private final Map<Long, Integer> breathConsecutiveMap = new ConcurrentHashMap<>();

    // 낙상 이벤트 쿨다운 — 5분 이내 중복 이벤트 방지
    private static final long FALL_COOLDOWN_MS = 5 * 60 * 1000L;
    private final Map<Long, Long> lastFallEventTimeMap = new ConcurrentHashMap<>();

    // 심박/호흡 이상 지속 시 재알림 간격 — 1분
    private static final long VITAL_RECALERT_MS = 60 * 1000L;

    // 진행 중인 심박/호흡 이상 episode — userId별 1건
    private final Map<Long, Episode> activeEpisodeMap = new ConcurrentHashMap<>();

    // 진행 중인 이상 episode 상태 — 신규 발생/에스컬레이션/재알림 판단에 사용
    private static class Episode {
        final Long eventId;
        final String severity;   // "WARNING"(한쪽 이상) | "CRITICAL"(양쪽 동시 이상)
        final String title;
        final String message;
        long lastAlertTime;

        Episode(Long eventId, String severity, String title, String message, long lastAlertTime) {
            this.eventId = eventId;
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
    private final FcmService fcmService;

    /**
     * 라즈베리파이 LCD [알림] 버튼 — 피보호자가 직접 도움 요청.
     * 자동 감지 조건과 무관하게 무조건 emergency_event 생성 + 활성 보호자 전원에게 즉시 알림 발송.
     * 중복 가드 없음 — 버튼을 누를 때마다 매번 새 이벤트/알림 생성.
     */
    @Transactional
    public ResponseEvent createManualAlert(String serialNum) {
        Device device = deviceRepository.findBySerialNum(serialNum)
            .orElseThrow(() -> new IllegalArgumentException("기기를 찾을 수 없습니다."));
        Users user = device.getUser();

        EmergencyEvent event = EmergencyEvent.builder()
            .user(user)
            .eventType("MANUAL_ALERT")
            .status("PENDING")
            .severity("CRITICAL")
            .device(device)
            .build();
        emergencyEventRepository.save(event);

        String title = "도움 요청";
        String message = user.getName() + "님이 도움을 요청했습니다";
        createNotificationsAndPush(event, user, title, message);

        return new ResponseEvent(event.getId(), event.getEventType(), event.getStatus(), event.getCreatedAt());
    }

    // 낙상 감지 — 5분 쿨다운으로 중복 이벤트 방지, CRITICAL 이벤트 저장 + 보호자 알림 + FCM 발송
    @Transactional
    public boolean checkFallEvent(VitalRequestDto vitalDto, Users user, Device device) {
        if (!vitalDto.isFallDetected()) return false;

        Long userId = user.getId();
        long now = System.currentTimeMillis();
        Long lastFallTime = lastFallEventTimeMap.get(userId);

        if (lastFallTime != null && now - lastFallTime < FALL_COOLDOWN_MS) {
            return true; // 쿨다운 중 — 이벤트 생성 없이 긴급 상태만 유지
        }

        lastFallEventTimeMap.put(userId, now);
        EmergencyEvent fallEvent = EmergencyEvent.builder()
            .user(user)
            .eventType("FALL")
            .status("PENDING")
            .severity("CRITICAL")
            .device(device)
            .build();
        emergencyEventRepository.save(fallEvent);

        String message = user.getName() + "낙상이 감지되었습니다. [심각] 즉시 확인이 필요합니다.";
        createNotificationsAndPush(fallEvent, user, "낙상 감지", message);
        return true;
    }

    // 심박수/호흡수 이상 감지 — 개인 기준선(7일 중앙값) 기반, 30초 연속 이상부터 알림
    // 한쪽만 이상 → WARNING(HEART_ISSUE/BREATH_ISSUE), 양쪽 동시 이상 → CRITICAL(VITAL_ISSUE)
    // 개인 데이터 3일 미만 시 고령자 표준 임계값으로 자동 전환
    @Transactional
    public boolean checkHeartBreathEvent(int heartRate, int breathRate, Users user, Device device) {
        Long userId = user.getId();
        int[] baseline = getPersonalBaseline(user);

        // 연속 이상 카운터 갱신 — 이상이면 +1, 정상이면 0으로 리셋
        int hrCount;
        if (isAbnormal(heartRate, baseline, true)) {
            hrCount = heartConsecutiveMap.merge(userId, 1, Integer::sum);
        } else {
            heartConsecutiveMap.put(userId, 0);
            hrCount = 0;
        }
        int brCount;
        if (isAbnormal(breathRate, baseline, false)) {
            brCount = breathConsecutiveMap.merge(userId, 1, Integer::sum);
        } else {
            breathConsecutiveMap.put(userId, 0);
            brCount = 0;
        }

        boolean hrBad = hrCount >= ABNORMAL_CONSECUTIVE;
        boolean brBad = brCount >= ABNORMAL_CONSECUTIVE;

        // 이상 지표 개수로 심각도 결정 — 양쪽 동시 이상이면 CRITICAL, 한쪽만이면 WARNING
        if (hrBad && brBad) {
            return handleCondition(user, device, "CRITICAL", "VITAL_ISSUE");
        } else if (hrBad) {
            return handleCondition(user, device, "WARNING", "HEART_ISSUE");
        } else if (brBad) {
            return handleCondition(user, device, "WARNING", "BREATH_ISSUE");
        }

        // 양쪽 다 30초 미달 — 이상 없음, 진행 중 episode 종료
        activeEpisodeMap.remove(userId);
        return false;
    }

    // 이상 상태 처리 — 신규 발생/악화 시 새 이벤트+알림, 지속 시 1분마다 미확인 보호자에게만 재알림
    private boolean handleCondition(Users user, Device device, String severity, String eventType) {
        Long userId = user.getId();
        Episode active = activeEpisodeMap.get(userId);
        long now = System.currentTimeMillis();

        boolean isNew = (active == null);
        boolean isEscalation = active != null
            && "WARNING".equals(active.severity) && "CRITICAL".equals(severity);

        if (isNew || isEscalation) {
            // 신규 발생 또는 WARNING→CRITICAL 악화 — 새 이벤트 생성 + 보호자 전원 알림
            EmergencyEvent event = EmergencyEvent.builder()
                .user(user).eventType(eventType).status("PENDING")
                .severity(severity).device(device).build();
            emergencyEventRepository.save(event);

            String[] tm = buildTitleMessage(user, eventType);
            createNotificationsAndPush(event, user, tm[0], tm[1]);
            activeEpisodeMap.put(userId, new Episode(event.getId(), severity, tm[0], tm[1], now));
            return true;
        }

        // 동일/완화 상태 지속 — 1분마다 아직 안 읽은 보호자에게만 FCM 재발송
        if (now - active.lastAlertTime >= VITAL_RECALERT_MS) {
            reAlertUnreadGuardians(active.eventId, active.title, active.message);
            active.lastAlertTime = now;
        }
        return true;
    }

    // eventType별 FCM 제목/메시지 생성 — [0]=title, [1]=message
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
            default -> new String[]{
                "바이탈 이상", name + "님의 바이탈에 이상이 감지되었습니다."};
        };
    }

    // 진행 중인 이벤트에 대해 아직 안 읽은(readYN=N) 보호자에게만 FCM 재발송 — 새 알림 행은 만들지 않음
    private void reAlertUnreadGuardians(Long eventId, String title, String message) {
        EmergencyEvent event = emergencyEventRepository.findById(eventId).orElse(null);
        if (event == null) {
            return;
        }
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

    // 개인 기준선(중앙값) 반환 — 캐시 유효하면 재사용, 만료 시 재계산
    // 데이터 부족(3일 미만)이면 null 반환 → 고령자 표준 임계값 사용
    private int[] getPersonalBaseline(Users user) {
        Long userId = user.getId();
        Long lastCalc = baselineCacheTime.get(userId);

        if (lastCalc != null
            && System.currentTimeMillis() - lastCalc < BASELINE_CACHE_TTL
            && baselineCache.containsKey(userId)) {
            return baselineCache.get(userId);
        }

        long count = vitalRepository.countByUserAndRecordedAtAfter(user, LocalDateTime.now().minusDays(7));
        if (count < MIN_RECORDS_FOR_BASELINE) {
            return null;
        }

        List<Vital> vitals = vitalRepository.findByUserAndRecordedAtAfter(user, LocalDateTime.now().minusDays(7));
        int medianHR = calculateMedian(vitals.stream().map(Vital::getHeartRate).sorted().toList());
        int medianBR = calculateMedian(vitals.stream().map(Vital::getBreathRate).sorted().toList());

        int[] baseline = {medianHR, medianBR};
        baselineCache.put(userId, baseline);
        baselineCacheTime.put(userId, System.currentTimeMillis());
        return baseline;
    }

    // 이상 여부 판단 — baseline null이면 고령자 표준 임계값, 있으면 개인 중앙값 ±15%
    private boolean isAbnormal(int value, int[] baseline, boolean isHeart) {
        if (baseline == null) {
            return isHeart
                ? (value < ELDERLY_HR_WARN_LOW || value > ELDERLY_HR_WARN_HIGH)
                : (value < ELDERLY_BR_WARN_LOW || value > ELDERLY_BR_WARN_HIGH);
        }
        int median = isHeart ? baseline[0] : baseline[1];
        return value < median * (1 - WARNING_RATIO) || value > median * (1 + WARNING_RATIO);
    }

    private int calculateMedian(List<Integer> sorted) {
        int size = sorted.size();
        if (size == 0) return 0;
        return size % 2 == 0
            ? (sorted.get(size / 2 - 1) + sorted.get(size / 2)) / 2
            : sorted.get(size / 2);
    }

    // 이벤트 발생 시 활성화된 모든 보호자에게 Notification 저장 + FCM 푸시 전송
    // 주의/심각/낙상 모두 긴급 알림으로 처리 — notificationType: "EMERGENCY"
    private void createNotificationsAndPush(EmergencyEvent event, Users user, String fcmTitle, String message) {
        String notificationType = "EMERGENCY";

        List<UserGuardianMap> mappings = userGuardianMapRepository.findByUser(user).stream()
            .filter(m -> "Y".equals(m.getIsActive()))
            .toList();

        for (UserGuardianMap mapping : mappings) {
            Guardian guardian = mapping.getGuardian();
            notificationRepository.save(Notification.builder()
                .emergencyEvent(event)
                .guardian(guardian)
                .message(message)
                .status("SUCCESS")
                .build());
            fcmService.sendPushNotification(guardian.getFcmToken(), fcmTitle, message, notificationType);
        }
    }

    // 보호자 ID로 담당 피보호자 전체의 해결/미해결 이벤트 수를 집계하여 반환
    @Transactional(readOnly = true)
    public EventSummaryResponse getEventSummaryForGuardian(Long guardianId) {
        Guardian guardian = guardianRepository.findById(guardianId)
            .orElseThrow(() -> new IllegalArgumentException("보호자를 찾을 수 없습니다."));

        List<Users> users = userGuardianMapRepository.findByGuardian(guardian).stream()
            .map(UserGuardianMap::getUser)
            .toList();

        if (users.isEmpty()) return new EventSummaryResponse(0, 0);

        long unresolvedCount = emergencyEventRepository.countByUserInAndStatus(users, "PENDING");
        long resolvedCount = emergencyEventRepository.countByUserInAndStatus(users, "RESOLVED");

        return new EventSummaryResponse(resolvedCount, unresolvedCount);
    }

    // 보호자 ID로 담당 피보호자 전체의 응급 이벤트 목록을 최신순으로 조회
    @Transactional(readOnly = true)
    public EmergencyAlertListResponse getEmergencyAlertsForGuardian(Long guardianId) {
        Guardian guardian = guardianRepository.findById(guardianId)
            .orElseThrow(() -> new IllegalArgumentException("보호자를 찾을 수 없습니다."));

        List<Users> users = userGuardianMapRepository.findByGuardian(guardian).stream()
            .map(UserGuardianMap::getUser)
            .toList();

        if (users.isEmpty()) return new EmergencyAlertListResponse(List.of());

        List<EmergencyAlertResponse> alerts = emergencyEventRepository
            .findByUserInOrderByCreatedAtDesc(users).stream()
            .map(event -> new EmergencyAlertResponse(
                event.getId(),
                event.getUser().getName(),
                event.getEventType(),
                event.getStatus(),
                event.getCreatedAt(),
                event.getEventDescription())
            ).toList();

        return new EmergencyAlertListResponse(alerts);
    }

    // 특정 이벤트를 RESOLVED로 변경 — 권한 검증 후 처리
    @Transactional
    public ResponseEvent resolveEvent(Long guardianId, Long eventId) {
        Guardian guardian = guardianRepository.findById(guardianId)
            .orElseThrow(() -> new IllegalArgumentException("보호자를 찾을 수 없습니다."));

        EmergencyEvent event = emergencyEventRepository.findById(eventId)
            .orElseThrow(() -> new IllegalArgumentException("응급 이벤트를 찾을 수 없습니다."));

        boolean authorized = userGuardianMapRepository
            .findByGuardianAndIsActive(guardian, "Y").stream()
            .anyMatch(m -> m.getUser().getId().equals(event.getUser().getId()));

        if (!authorized) {
            throw new IllegalStateException("해당 이벤트를 해결할 권한이 없습니다.");
        }

        if (!"RESOLVED".equals(event.getStatus())) {
            event.updateStatus("RESOLVED");
            fcmService.sendPushNotification(
                guardian.getFcmToken(),
                "응급 상황 해결",
                event.getUser().getName() + "님의 응급 상황이 해결되었습니다.",
                "ALERT"
            );
        }

        return new ResponseEvent(event.getId(), event.getEventType(), event.getStatus(), event.getCreatedAt());
    }

    // 보호자 ID로 담당 피보호자 전체의 PENDING 이벤트를 일괄 RESOLVED 처리
    @Transactional
    public void resolveAllEvents(Long guardianId) {
        Guardian guardian = guardianRepository.findById(guardianId)
            .orElseThrow(() -> new IllegalArgumentException("보호자를 찾을 수 없습니다."));

        List<Users> users = userGuardianMapRepository.findByGuardian(guardian).stream()
            .map(UserGuardianMap::getUser)
            .toList();

        if (users.isEmpty()) return;

        emergencyEventRepository.findByUserInAndStatus(users, "PENDING")
            .forEach(event -> event.updateStatus("RESOLVED"));

        users.forEach(user ->
            fcmService.sendPushNotification(
                guardian.getFcmToken(),
                "응급 상황 해결",
                user.getName() + "님의 모든 응급 상황이 해결되었습니다.",
                "ALERT"
            )
        );
    }

    // 피보호자 ID로 가장 최근 PENDING 이벤트 1건 조회
    @Transactional(readOnly = true)
    public ResponseEvent getLatestPendingEvent(long userId) {
        Users user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        return emergencyEventRepository.findTopByUserAndStatusOrderByCreatedAtDesc(user, "PENDING")
            .map(event -> new ResponseEvent(
                event.getId(),
                event.getEventType(),
                event.getStatus(),
                event.getCreatedAt()
            ))
            .orElse(null);
    }
}
