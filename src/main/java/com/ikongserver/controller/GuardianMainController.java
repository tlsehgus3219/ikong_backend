package com.ikongserver.controller;

import com.ikongserver.dto.ApiResponse;
import com.ikongserver.dto.GuardianMainDto.DashboardSummary;
import com.ikongserver.dto.GuardianMainDto.UserCard;
import com.ikongserver.dto.GuardianMainDto.UserListResponse;
import com.ikongserver.service.GuardianMainService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 보호자 메인 화면 API.
 * 모든 엔드포인트는 JWT 인증된 GUARDIAN 토큰을 요구한다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/guardians/me")
public class GuardianMainController {

    private final GuardianMainService guardianMainService;

    /**
     * 보호자 메인 화면 상단 카운트 (긴급/주의/외출/전체).
     */
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<DashboardSummary>> getDashboard() {
        Long guardianId = currentGuardianId();
        DashboardSummary summary = guardianMainService.getDashboard(guardianId);
        return ResponseEntity.ok(ApiResponse.ok(summary));
    }

    /**
     * 담당하는 피보호자 카드 목록.
     */
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<UserListResponse>> getMyUsers() {
        Long guardianId = currentGuardianId();
        UserListResponse response = guardianMainService.getMyUsers(guardianId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * 특정 피보호자 상세 (긴급 상황 UI에서 활용).
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<UserCard>> getUserDetail(@PathVariable Long userId) {
        Long guardianId = currentGuardianId();
        UserCard card = guardianMainService.getUserDetail(guardianId, userId);
        return ResponseEntity.ok(ApiResponse.ok(card));
    }

    /**
     * SecurityContext 에서 guardianId 추출.
     * JwtAuthenticationFilter 가 토큰의 subject(=guardianId 문자열)를 username 으로 세팅한다.
     */
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
