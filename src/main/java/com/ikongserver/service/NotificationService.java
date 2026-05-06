package com.ikongserver.service;

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

    // 응급 이벤트와 연결된 알림 생성 — status 미입력 시 기본값 "SUCCESS" 저장
    @Transactional
    public CreateNotificationResponse createNotification(CreateNotificationRequest request) {
        EmergencyEvent event = emergencyEventRepository.findById(request.eventId())
                .orElseThrow(() -> new RuntimeException("비상 이벤트를 찾을 수 없습니다."));

        Guardian guardian = guardianRepository.findById(request.guardianId())
                .orElseThrow(() -> new RuntimeException("보호자를 찾을 수 없습니다."));

        Notification notification = notificationRepository.save(
                Notification.builder()
                        .emergencyEvent(event)
                        .guardian(guardian)
                        .message(request.message())
                        .status(request.status() != null ? request.status() : "SUCCESS")
                        .build()
        );

        return new CreateNotificationResponse(notification.getId(), notification.getMessage(), notification.getSentAt());
    }

    // 보호자 ID 기준 알림 목록 조회 — status 파라미터가 있으면 상태 필터 적용, 없으면 전체 반환 (페이징)
    @Transactional(readOnly = true)
    public NotificationListResponse getNotifications(Long guardianId, String status, int page, int size) {
        Page<Notification> result;

        if (status != null && !status.isEmpty()) {
            result = notificationRepository.findByGuardianIdAndStatus(
                    guardianId, status, PageRequest.of(page - 1, size));
        } else {
            result = notificationRepository.findByGuardianId(
                    guardianId, PageRequest.of(page - 1, size));
        }

        return new NotificationListResponse(
                result.getTotalElements(),
                result.getContent().stream()
                        .map(n -> new NotificationItem(
                                n.getId(),
                                n.getEmergencyEvent().getUser().getName(),
                                n.getEmergencyEvent().getEventType(),
                                n.getMessage(),
                                n.getStatus(),
                                n.getSentAt(),
                                n.getReadYN()))
                        .toList()
        );
    }

    // 피보호자 ID 기준 알림 목록 조회 — 본인과 연결된 응급 이벤트의 알림만 반환 (페이징)
    @Transactional(readOnly = true)
    public NotificationListResponse getNotificationsByUserId(Long userId, String status, int page, int size) {
        Page<Notification> result;

        if (status != null && !status.isEmpty()) {
            result = notificationRepository.findByUserIdAndStatus(
                    userId, status, PageRequest.of(page - 1, size));
        } else {
            result = notificationRepository.findByUserId(
                    userId, PageRequest.of(page - 1, size));
        }

        return new NotificationListResponse(
                result.getTotalElements(),
                result.getContent().stream()
                        .map(n -> new NotificationItem(
                                n.getId(),
                                n.getEmergencyEvent().getUser().getName(),
                                n.getEmergencyEvent().getEventType(),
                                n.getMessage(),
                                n.getStatus(),
                                n.getSentAt(),
                                n.getReadYN()))
                        .toList()
        );
    }

    // 보호자의 읽지 않은 알림 수 반환
    public long getUnreadCount(Long guardianId) {
        return notificationRepository.countByGuardianIdAndReadYN(guardianId, "N");
    }

    // 특정 알림을 읽음 처리 — JPA dirty checking으로 별도 save() 없이 DB 반영
    @Transactional
    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("알림을 찾을 수 없습니다."));
        notification.updateReadYN("Y");
    }
}
