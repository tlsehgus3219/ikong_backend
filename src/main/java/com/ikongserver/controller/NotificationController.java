package com.ikongserver.controller;

import com.ikongserver.dto.NotificationDto.CreateNotificationRequest;
import com.ikongserver.dto.NotificationDto.CreateNotificationResponse;
import com.ikongserver.dto.NotificationDto.NotificationListResponse;
import com.ikongserver.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 알림(Notification) API 컨트롤러
 * - 알림 생성: IoT 기기/서버가 응급 이벤트 발생 시 호출 (인증 불필요)
 * - 알림 조회: 보호자는 자신에게 온 알림, 피보호자는 본인 관련 알림 조회
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // 응급 이벤트 발생 시 알림 생성 — IoT 기기나 내부 서버에서 호출, JWT 인증 불필요
    @PostMapping
    public ResponseEntity<CreateNotificationResponse> createNotification(
            @RequestBody CreateNotificationRequest request) {
        return ResponseEntity.ok(notificationService.createNotification(request));
    }

    // 알림 목록 조회 — ROLE_GUARDIAN이면 guardianId, 피보호자면 userId 기준으로 조회 (페이징, 상태 필터 지원)
    @GetMapping
    public ResponseEntity<NotificationListResponse> getNotifications(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        Long id = Long.parseLong(userDetails.getUsername());

        boolean isGuardian = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_GUARDIAN"));

        if (isGuardian) {
            // 보호자: guardianId 기준으로 알림 조회
            return ResponseEntity.ok(notificationService.getNotifications(id, status, page, size));
        } else {
            // 피보호자: userId 기준으로 연결된 응급 이벤트의 알림 조회
            return ResponseEntity.ok(notificationService.getNotificationsByUserId(id, status, page, size));
        }
    }

    // 보호자의 읽지 않은(readYN=N) 알림 수 반환 — 앱 뱃지 표시용
    @GetMapping("/unread-count")
    public ResponseEntity<Long> getUnreadCount(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long guardianId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(notificationService.getUnreadCount(guardianId));
    }

    // 특정 알림을 읽음(readYN=Y)으로 변경
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long notificationId) {
        notificationService.markAsRead(notificationId);
        return ResponseEntity.ok().build();
    }
}
