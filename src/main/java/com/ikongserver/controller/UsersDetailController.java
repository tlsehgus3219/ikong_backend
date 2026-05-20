package com.ikongserver.controller;

import com.ikongserver.dto.StatsDto.VitalStatsResponse;
import com.ikongserver.dto.UserDto.UserStateDetailResponse;
import com.ikongserver.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/guardians/me/users")
public class UsersDetailController {

    private final UserService userService;

    // 피보호자 생체 데이터 상태 상세 조회 (이름, 관계, 상태, 심박수, 호흡수, 업데이트 시간, 활동 상태)
    @GetMapping("/{userId}/detail")
    public ResponseEntity<UserStateDetailResponse> getUserProfileDetail(@PathVariable Long userId) {
        Long guardianId = currentGuardianId();
        UserStateDetailResponse response = userService.getUserProfileDetail(userId, guardianId);
        return ResponseEntity.ok(response);
    }

    // 생체 통계 그래프 조회 — type(HEART/BREATH), period(TODAY/WEEK/MONTH)
    // 응답: 평균/최소/최대 + 그래프 데이터(오름차순) + 하단 목록(내림차순)
    @GetMapping("/{userId}/stats")
    public ResponseEntity<VitalStatsResponse> getVitalStats(
        @PathVariable Long userId,
        @RequestParam(defaultValue = "HEART") String type,
        @RequestParam(defaultValue = "TODAY") String period) {
        Long guardianId = currentGuardianId();
        return ResponseEntity.ok(userService.getVitalStats(userId, guardianId, type, period));
    }

    // JWT 토큰의 subject(guardianId)를 SecurityContext에서 추출
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
