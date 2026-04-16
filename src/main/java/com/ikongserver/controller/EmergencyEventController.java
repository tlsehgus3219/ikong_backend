package com.ikongserver.controller;

import com.ikongserver.dto.EventDto.ResponseEvent;
import com.ikongserver.service.EmergencyEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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

}
