package com.ikongserver.controller;

import com.ikongserver.dto.EventDto.EmergencyAlertListResponse;
import com.ikongserver.dto.EventDto.EventSummaryResponse;
import com.ikongserver.dto.EventDto.ResponseEvent;
import com.ikongserver.service.EmergencyEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/emergency_event")
public class EmergencyEventController {

    private final EmergencyEventService emergencyEventService;

    // 응급 이슈 프론트 전달
    @GetMapping("{userId}/emergency")
    public ResponseEntity<ResponseEvent> getLatestEmergencyEvent(@PathVariable Long userId) {
        ResponseEvent response = emergencyEventService.getLatestPendingEvent(userId);
        return ResponseEntity.ok(response);
    }

    // 보호자 기준 해결건/미해결건 요약
    @GetMapping("/summary")
    public ResponseEntity<EventSummaryResponse> getEventSummary(
        @AuthenticationPrincipal UserDetails userDetails) {
        Long guardianId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(emergencyEventService.getEventSummaryForGuardian(guardianId));
    }

    // 보호자 기준 긴급 알림 목록
    @GetMapping("/alerts")
    public ResponseEntity<EmergencyAlertListResponse> getEmergencyAlerts(
        @AuthenticationPrincipal UserDetails userDetails) {
        Long guardianId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(emergencyEventService.getEmergencyAlertsForGuardian(guardianId));
    }

    // 개별 이벤트 해결 처리
    @PatchMapping("/{eventId}/resolve")
    public ResponseEntity<ResponseEvent> resolveEvent(
        @AuthenticationPrincipal UserDetails userDetails,
        @PathVariable Long eventId) {
        Long guardianId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(emergencyEventService.resolveEvent(guardianId, eventId));
    }

    // 보호자 기준 전체 이벤트 해결 처리
    @PatchMapping("/resolve-all")
    public ResponseEntity<Void> resolveAllEvents(
        @AuthenticationPrincipal UserDetails userDetails) {
        Long guardianId = Long.parseLong(userDetails.getUsername());
        emergencyEventService.resolveAllEvents(guardianId);
        return ResponseEntity.ok().build();
    }
}
