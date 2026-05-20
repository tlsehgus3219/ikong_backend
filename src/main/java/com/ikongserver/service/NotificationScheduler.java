package com.ikongserver.service;

import com.ikongserver.entity.EmergencyEvent;
import com.ikongserver.entity.Notification;
import com.ikongserver.repository.EmergencyEventRepository;
import com.ikongserver.repository.NotificationRepository;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 미확인(PENDING) 응급 이벤트에 대해 1분마다 보호자에게 재알림을 보내는 스케줄러.
 * - 보호자가 [해결] 버튼을 눌러 RESOLVED 처리하면 자연스럽게 재알림 중단
 * - 첫 알림 후 1분 미만이면 건너뜀 (중복 방지)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private static final long RESEND_INTERVAL_MINUTES = 1;

    private final EmergencyEventRepository emergencyEventRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;

    /**
     * 매 1분마다 실행.
     * - 모든 PENDING 응급 이벤트를 조회
     * - 각 이벤트의 마지막 알림이 1분 이상 지났으면 활성 보호자에게 재알림 생성
     * - 메시지는 첫 알림에서 사용한 그대로 재사용 (수치 일관성 유지)
     */
    @Scheduled(fixedDelay = 60_000L)
    public void resendUnacknowledgedNotifications() {
        List<EmergencyEvent> pendingEvents = emergencyEventRepository.findAllByStatus("PENDING");
        if (pendingEvents.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        int resent = 0;

        for (EmergencyEvent event : pendingEvents) {
            Optional<Notification> lastOpt =
                notificationRepository.findTopByEmergencyEventOrderBySentAtDesc(event);

            // 알림 한 번도 안 나간 이벤트는 첫 알림에서 처리하므로 스케줄러는 건너뜀
            if (lastOpt.isEmpty()) {
                continue;
            }

            long minutesSince = ChronoUnit.MINUTES.between(lastOpt.get().getSentAt(), now);
            if (minutesSince < RESEND_INTERVAL_MINUTES) {
                continue;
            }

            // 첫 알림의 메시지 재사용
            String message = notificationRepository
                .findTopByEmergencyEventOrderBySentAtAsc(event)
                .map(Notification::getMessage)
                .orElse("미확인 응급 상황이 있습니다");

            notificationService.createForEvent(event, message);
            resent++;
        }

        if (resent > 0) {
            log.info("[NotificationScheduler] PENDING 이벤트 {}건 재알림 발송", resent);
        }
    }
}
