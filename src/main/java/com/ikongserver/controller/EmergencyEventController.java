package com.ikongserver.controller;

import com.ikongserver.dto.ApiResponse;
import com.ikongserver.dto.EventDto.ResponseEvent;
import com.ikongserver.service.EmergencyEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    public ResponseEntity<ResponseEvent> getLatestEmergencyEvent(
        @PathVariable Long userId) {

        ResponseEvent response = emergencyEventService.getLatestPendingEvent(userId);

        // 데이터가 없으면 204 No Content를 주거나, null을 포함한 200 OK를 줍니다.
        return ResponseEntity.ok(response);

    }

    /**
     * 보호자가 응급 이벤트 1건을 해결 처리.
     * 토큰의 guardianId 기준으로 권한 체크 후 status를 RESOLVED 로 변경.
     */
    @PatchMapping("/{eventId}/resolve")
    public ResponseEntity<ApiResponse<ResponseEvent>> resolveEvent(
        @PathVariable Long eventId
    ) {
        Long guardianId = currentGuardianId();
        ResponseEvent response = emergencyEventService.resolveEvent(guardianId, eventId);
        return ResponseEntity.ok(ApiResponse.ok("응급 이벤트가 해결 처리되었습니다.", response));
    }

    private Long currentGuardianId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new IllegalStateException("인증 정보가 없습니다.");
        }
        try {
            return Long.parseLong(auth.getName());
        } catch (NumberFormatException e) {
            throw new IllegalStateException("유효하지 않은 토큰입니다.");
        }
    }

}
