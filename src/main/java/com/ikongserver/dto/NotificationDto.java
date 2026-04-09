package com.ikongserver.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class NotificationDto {

    @Getter
    @NoArgsConstructor
    public static class CreateNotificationRequest {
        private Long eventId;
        private Long guardianId;
        private String message;
        private String status; // SUCCESS | FAIL (기본값: SUCCESS)
    }

    @Getter
    @Builder
    public static class CreateNotificationResponse {
        private Long notificationId;
        private String message;
        private LocalDateTime sentAt;
    }

    @Getter
    @Builder
    public static class NotificationItem {
        private Long notificationId;
        private String message;
        private String status;
        private LocalDateTime sentAt;
        private String readYN;
    }

    @Getter
    @Builder
    public static class NotificationListResponse {
        private long total;
        private List<NotificationItem> notifications;
    }
}
