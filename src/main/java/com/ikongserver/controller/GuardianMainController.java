package com.ikongserver.controller;

import com.ikongserver.dto.ApiResponse;
import com.ikongserver.dto.GuardianDto;
import com.ikongserver.dto.GuardianMainDto.DashboardSummary;
import com.ikongserver.dto.GuardianMainDto.UserCard;
import com.ikongserver.dto.GuardianMainDto.UserListResponse;
import com.ikongserver.service.GuardianMainService;
import com.ikongserver.service.GuardianService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/guardians/me")
public class GuardianMainController {

    private final GuardianMainService guardianMainService;
    private final GuardianService guardianService;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<DashboardSummary>> getDashboard() {
        Long guardianId = currentGuardianId();
        DashboardSummary summary = guardianMainService.getDashboard(guardianId);
        return ResponseEntity.ok(ApiResponse.ok(summary));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<UserListResponse>> getMyUsers() {
        Long guardianId = currentGuardianId();
        UserListResponse response = guardianMainService.getMyUsers(guardianId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<UserCard>> getUserDetail(@PathVariable Long userId) {
        Long guardianId = currentGuardianId();
        UserCard card = guardianMainService.getUserDetail(guardianId, userId);
        return ResponseEntity.ok(ApiResponse.ok(card));
    }

    @GetMapping("/invitations")
    public ResponseEntity<ApiResponse<List<GuardianDto.PendingInvitationResponse>>> getPendingInvitations() {
        Long guardianId = currentGuardianId();
        List<GuardianDto.PendingInvitationResponse> list = guardianService.getPendingInvitations(guardianId);
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @PostMapping("/invitations/{invitationId}/accept")
    public ResponseEntity<ApiResponse<Void>> acceptInvitation(@PathVariable Long invitationId) {
        guardianService.acceptInvitation(invitationId);
        return ResponseEntity.ok(ApiResponse.ok("초대를 수락했습니다.", null));
    }

    @PostMapping("/invitations/{invitationId}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectInvitation(@PathVariable Long invitationId) {
        guardianService.rejectInvitation(invitationId);
        return ResponseEntity.ok(ApiResponse.ok("초대를 거절했습니다.", null));
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
