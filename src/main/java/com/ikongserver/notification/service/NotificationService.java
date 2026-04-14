package com.ikongserver.notification.service;

import com.ikongserver.dto.NotificationDto.CreateNotificationRequest;
import com.ikongserver.dto.NotificationDto.CreateNotificationResponse;
import com.ikongserver.dto.NotificationDto.NotificationItem;
import com.ikongserver.dto.NotificationDto.NotificationListResponse;
import com.ikongserver.entity.EmergencyEvent;
import com.ikongserver.entity.Guardian;
import com.ikongserver.entity.Notification;
import com.ikongserver.repository.EmergencyEventRepository;
import com.ikongserver.repository.GuardianRepository;
import com.ikongserver.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmergencyEventRepository emergencyEventRepository;
    private final GuardianRepository guardianRepository;

    @Transactional
    public CreateNotificationResponse createNotification(CreateNotificationRequest request) {
        EmergencyEvent event = emergencyEventRepository.findById(request.getEventId())
                .orElseThrow(() -> new RuntimeException("비상 이벤트를 찾을 수 없습니다."));

        Guardian guardian = guardianRepository.findById(request.getGuardianId())
                .orElseThrow(() -> new RuntimeException("보호자를 찾을 수 없습니다."));

        Notification notification = notificationRepository.save(
                Notification.builder()
                        .emergencyEvent(event)
                        .guardian(guardian)
                        .message(request.getMessage())
                        .status(request.getStatus() != null ? request.getStatus() : "SUCCESS")
                        .build()
        );

        return CreateNotificationResponse.builder()
                .notificationId(notification.getId())
                .message(notification.getMessage())
                .sentAt(notification.getSentAt())
                .build();
    }

    public NotificationListResponse getNotifications(Long guardianId, String status, int page, int size) {
        Page<Notification> result;

        if (status != null && !status.isEmpty()) {
            result = notificationRepository.findByGuardianIdAndStatus(
                    guardianId, status, PageRequest.of(page - 1, size));
        } else {
            result = notificationRepository.findByGuardianId(
                    guardianId, PageRequest.of(page - 1, size));
        }

        return NotificationListResponse.builder()
                .total(result.getTotalElements())
                .notifications(result.getContent().stream()
                        .map(n -> NotificationItem.builder()
                                .notificationId(n.getId())
                                .message(n.getMessage())
                                .status(n.getStatus())
                                .sentAt(n.getSentAt())
                                .readYN(n.getReadYN())
                                .build())
                        .toList())
                .build();
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("알림을 찾을 수 없습니다."));
        notification.updateReadYN("Y");
    }
}
