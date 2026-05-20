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
    private static final int ELDERLY_HR_CRIT_LOW = 55, ELDERLY_HR_CRIT_HIGH = 120;
    private static final int ELDERLY_BR_WARN_LOW = 12, ELDERLY_BR_WARN_HIGH = 20;
    private static final int ELDERLY_BR_CRIT_LOW = 10, ELDERLY_BR_CRIT_HIGH = 25;

    // 개인 기준선 임계 비율 — 중앙값 기준 ±15% WARNING, ±30% CRITICAL
    private static final double WARNING_RATIO = 0.15;
    private static final double CRITICAL_RATIO = 0.30;

    // 연속 이상 감지 기준 — 초 단위 (매초 데이터 수신 기준)
    private static final int WARNING_CONSECUTIVE = 30;  // 30초 연속 이상
    private static final int CRITICAL_CONSECUTIVE = 60; // 60초 연속 이상

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

    // 심박수/호흡수 CRITICAL 지속 시 5분마다 재알림
    private static final long VITAL_RECALERT_MS = 5 * 60 * 1000L;
    private final Map<Long, Long> lastHeartEventTimeMap = new ConcurrentHashMap<>();
    private final Map<Long, Long> lastBreathEventTimeMap = new ConcurrentHashMap<>();

    private final EmergencyEventRepository emergencyEventRepository;
    private final UsersRepository userRepository;
    private final GuardianRepository guardianRepository;
    private final UserGuardianMapRepository userGuardianMapRepository;
    private final NotificationRepository notificationRepository;
    private final VitalRepository vitalRepository;
    private final FcmService fcmService;

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

    // 심박수/호흡수 이상 감지 — 개인 기준선(7일 중앙값) 기반, 연속 30초→WARNING / 60초→CRITICAL
    // 개인 데이터 3일 미만 시 고령자 표준 임계값으로 자동 전환
    @Transactional
    public boolean checkHeartBreathEvent(VitalRequestDto vitalDto, Users user, Device device) {
        boolean isIssueDetected = false;
        Long userId = user.getId();
        int[] baseline = getPersonalBaseline(user);

        // 심박수 연속 이상 감지 — 30초 WARNING, 60초 CRITICAL, 이후 5분마다 CRITICAL 재알림
        int hr = vitalDto.heartRate();
        if (isAbnormal(hr, baseline, true)) {
            int count = heartConsecutiveMap.merge(userId, 1, Integer::sum);
            if (count == WARNING_CONSECUTIVE) {
                String severity = isCritical(hr, baseline, true) ? "CRITICAL" : "WARNING";
                saveHeartEvent(user, device, severity);
                lastHeartEventTimeMap.put(userId, System.currentTimeMillis());
                isIssueDetected = true;
            } else if (count == CRITICAL_CONSECUTIVE) {
                saveHeartEvent(user, device, "CRITICAL");
                lastHeartEventTimeMap.put(userId, System.currentTimeMillis());
                isIssueDetected = true;
            } else if (count > CRITICAL_CONSECUTIVE) {
                long now = System.currentTimeMillis();
                Long lastTime = lastHeartEventTimeMap.get(userId);
                if (lastTime == null || now - lastTime >= VITAL_RECALERT_MS) {
                    saveHeartEvent(user, device, "CRITICAL");
                    lastHeartEventTimeMap.put(userId, now);
                    isIssueDetected = true;
                }
            }
        } else {
            heartConsecutiveMap.put(userId, 0);
        }

        // 호흡수 연속 이상 감지 — 30초 WARNING, 60초 CRITICAL, 이후 5분마다 CRITICAL 재알림
        int br = vitalDto.breathRate();
        if (isAbnormal(br, baseline, false)) {
            int count = breathConsecutiveMap.merge(userId, 1, Integer::sum);
            if (count == WARNING_CONSECUTIVE) {
                String severity = isCritical(br, baseline, false) ? "CRITICAL" : "WARNING";
                saveBreathEvent(user, device, severity);
                lastBreathEventTimeMap.put(userId, System.currentTimeMillis());
                isIssueDetected = true;
            } else if (count == CRITICAL_CONSECUTIVE) {
                saveBreathEvent(user, device, "CRITICAL");
                lastBreathEventTimeMap.put(userId, System.currentTimeMillis());
                isIssueDetected = true;
            } else if (count > CRITICAL_CONSECUTIVE) {
                long now = System.currentTimeMillis();
                Long lastTime = lastBreathEventTimeMap.get(userId);
                if (lastTime == null || now - lastTime >= VITAL_RECALERT_MS) {
                    saveBreathEvent(user, device, "CRITICAL");
                    lastBreathEventTimeMap.put(userId, now);
                    isIssueDetected = true;
                }
            }
        } else {
            breathConsecutiveMap.put(userId, 0);
        }

        return isIssueDetected;
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

    // 심각 여부 판단 — baseline null이면 고령자 표준 임계값, 있으면 개인 중앙값 ±30%
    private boolean isCritical(int value, int[] baseline, boolean isHeart) {
        if (baseline == null) {
            return isHeart
                ? (value < ELDERLY_HR_CRIT_LOW || value > ELDERLY_HR_CRIT_HIGH)
                : (value < ELDERLY_BR_CRIT_LOW || value > ELDERLY_BR_CRIT_HIGH);
        }
        int median = isHeart ? baseline[0] : baseline[1];
        return value < median * (1 - CRITICAL_RATIO) || value > median * (1 + CRITICAL_RATIO);
    }

    private int calculateMedian(List<Integer> sorted) {
        int size = sorted.size();
        if (size == 0) return 0;
        return size % 2 == 0
            ? (sorted.get(size / 2 - 1) + sorted.get(size / 2)) / 2
            : sorted.get(size / 2);
    }

    private void saveHeartEvent(Users user, Device device, String severity) {
        EmergencyEvent event = EmergencyEvent.builder()
            .user(user).eventType("HEART_ISSUE").status("PENDING").severity(severity).device(device).build();
        emergencyEventRepository.save(event);
        boolean isCritical = "CRITICAL".equals(severity);
        String title = isCritical ? "[심각] 심박수 위험" : "[주의] 심박수 이상";
        String message = isCritical
            ? user.getName() + "심박수가 위험 수준입니다. 즉시 확인이 필요합니다."
            : user.getName() + "심박수가 평소와 다르게 측정되고 있습니다. 상태를 주의깊게 살펴보세요.";
        createNotificationsAndPush(event, user, title, message);
    }

    private void saveBreathEvent(Users user, Device device, String severity) {
        EmergencyEvent event = EmergencyEvent.builder()
            .user(user).eventType("BREATH_ISSUE").status("PENDING").severity(severity).device(device).build();
        emergencyEventRepository.save(event);
        boolean isCritical = "CRITICAL".equals(severity);
        String title = isCritical ? "[심각] 호흡수 위험" : "[주의] 호흡수 이상";
        String message = isCritical
            ? user.getName() + "호흡수가 위험 수준입니다. 즉시 확인이 필요합니다."
            : user.getName() + "호흡수가 평소와 다르게 측정되고 있습니다. 상태를 주의깊게 살펴보세요.";
        createNotificationsAndPush(event, user, title, message);
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

        LocalDateTime tenMinutesAgo = LocalDateTime.now().minusMinutes(10);
        return emergencyEventRepository
            .findTopByUserAndStatusAndCreatedAtAfterOrderByCreatedAtDesc(user, "PENDING", tenMinutesAgo)
            .map(event -> new ResponseEvent(
                event.getId(),
                event.getEventType(),
                event.getStatus(),
                event.getCreatedAt()
            ))
            .orElse(null);
    }
}
