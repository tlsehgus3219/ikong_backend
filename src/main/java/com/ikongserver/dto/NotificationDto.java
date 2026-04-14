package com.ikongserver.dto;

import java.time.LocalDateTime;
import java.util.List;

public class NotificationDto {

    public record CreateNotificationRequest(Long eventId, Long guardianId, String message, String status) {}

    public record CreateNotificationResponse(Long notificationId, String message, LocalDateTime sentAt) {}

    public record NotificationItem(Long notificationId, String message, String status, LocalDateTime sentAt, String readYN) {}

    public record NotificationListResponse(long total, List<NotificationItem> notifications) {}
}
