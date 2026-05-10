package com.ikongserver.dto;

import java.time.LocalDateTime;
import java.util.List;

public class NotificationDto {

    public record CreateNotificationRequest(Long eventId, Long guardianId, String message,
                                            String status) {

    }

    public record CreateNotificationResponse(Long notificationId, String message,
                                             LocalDateTime sentAt) {

    }

    public record NotificationItem(Long notificationId, String userName, String eventType,
                                   String message, String status, LocalDateTime sentAt,
                                   String readYN) {

    }

    public record NotificationListResponse(long total, List<NotificationItem> notifications) {

    }

    // 긴급 알림 디테일 (이름, event 유형, 상세 내용, 발생 시각)
    public record EmergencyEventDetailResponse(Long userId, String name, Long eventId,
                                               String eventType,
                                               String description, LocalDateTime occurredAt) {

    }
}
