package com.ikongserver.controller;

import com.ikongserver.dto.UserConditionDto.ConditionsResponse;
import com.ikongserver.dto.UserConditionDto.UpdateConditionsRequest;
import com.ikongserver.service.UserConditionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/conditions")
public class UserConditionController {

    private final UserConditionService userConditionService;

    // 현재 등록된 질환 목록 조회
    @GetMapping
    public ResponseEntity<ConditionsResponse> getConditions(
        @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(userConditionService.getConditions(userId));
    }

    // 질환 목록 등록/수정/삭제 — 빈 리스트 전송 시 전체 삭제
    @PutMapping
    public ResponseEntity<ConditionsResponse> updateConditions(
        @AuthenticationPrincipal UserDetails userDetails,
        @RequestBody UpdateConditionsRequest request) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(userConditionService.updateConditions(userId, request));
    }
}
