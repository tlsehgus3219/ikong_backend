package com.ikongserver.controller;

import com.ikongserver.dto.EventDto.EmergencyAlertListResponse;
import com.ikongserver.dto.EventDto.EventSummaryResponse;
import com.ikongserver.dto.EventDto.ManualAlertRequest;
import com.ikongserver.dto.EventDto.ResponseEvent;
import com.ikongserver.dto.NotificationDto.EmergencyEventDetailResponse;
import com.ikongserver.service.EmergencyEventService;
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
import org.springframework.web.bind.annotation.RestController;

/**
 * 응급 이벤트 API 컨트롤러 - 피보호자 앱: 본인의 미해결 이벤트 조회 - 보호자 앱: 이벤트 요약, 목록 조회, 해결 처리
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/emergency_event")
public class EmergencyEventController {

    private final EmergencyEventService emergencyEventService;
    private final NotificationService notificationService;

    // [피보호자용] 본인의 가장 최근 미처리(PENDING) 응급 이벤트 1건 반환 — 앱 화면에 팝업 표시용
    @GetMapping("{userId}/emergency")
    public ResponseEntity<ResponseEvent> getLatestEmergencyEvent(@PathVariable Long userId) {
        ResponseEvent response = emergencyEventService.getLatestPendingEvent(userId);
        return ResponseEntity.ok(response);
    }

    // [라즈베리파이용] LCD [알림] 버튼 → 활성 보호자 전원에게 즉시 도움 요청 알림 발송
    @PostMapping("/manual")
    public ResponseEntity<ResponseEvent> createManualAlert(@RequestBody ManualAlertRequest request) {
        ResponseEvent response = emergencyEventService.createManualAlert(request.serialNum());
        return ResponseEntity.ok(response);
    }

    // [보호자용] 담당 피보호자 전체의 해결된 이벤트 수 / 미해결 이벤트 수 요약 반환
    @GetMapping("/summary")
    public ResponseEntity<EventSummaryResponse> getEventSummary(
        @AuthenticationPrincipal UserDetails userDetails) {
        Long guardianId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(emergencyEventService.getEventSummaryForGuardian(guardianId));
    }

    // [보호자용] 담당 피보호자 전체의 응급 이벤트 목록을 최신순으로 반환
    @GetMapping("/alerts")
    public ResponseEntity<EmergencyAlertListResponse> getEmergencyAlerts(
        @AuthenticationPrincipal UserDetails userDetails) {
        Long guardianId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(emergencyEventService.getEmergencyAlertsForGuardian(guardianId));
    }

    // [보호자용] 특정 이벤트를 RESOLVED로 변경 — 권한 없는 보호자가 처리하면 403 반환
    @PatchMapping("/{eventId}/resolve")
    public ResponseEntity<ResponseEvent> resolveEvent(
        @AuthenticationPrincipal UserDetails userDetails,
        @PathVariable Long eventId) {
        Long guardianId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(emergencyEventService.resolveEvent(guardianId, eventId));
    }

    // [보호자용] 담당 피보호자의 미해결(PENDING) 이벤트 전체를 한 번에 RESOLVED 처리
    @PatchMapping("/resolve-all")
    public ResponseEntity<Void> resolveAllEvents(
        @AuthenticationPrincipal UserDetails userDetails) {
        Long guardianId = Long.parseLong(userDetails.getUsername());
        emergencyEventService.resolveAllEvents(guardianId);
        return ResponseEntity.ok().build();
    }

    // [보호자용] 긴급 알림 디테일 상황
    @GetMapping("{eventId}/detail")
    public ResponseEntity<EmergencyEventDetailResponse> getEmergencyEventDetail(
        @PathVariable Long eventId,
        @AuthenticationPrincipal UserDetails userDetails) {
        Long guardianId = Long.parseLong(userDetails.getUsername());
        // 긴급 알림 디테일 데이터
        EmergencyEventDetailResponse response = notificationService.getEmergencyEventDetail(eventId,
            guardianId);
        // 200 OK와 함께 DTO 반환
        return ResponseEntity.ok(response);
    }

}
