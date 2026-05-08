package com.ikongserver.controller;

import com.ikongserver.dto.UserDto.UserStateDetailResponse;
import com.ikongserver.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/guardians/me/users")
public class UsersDetailController {

    private final UserService userService;

    // 보호자 화면에서 피보호자 생체 데이터 확인
    @GetMapping("/{userId}/detail")
    public ResponseEntity<UserStateDetailResponse> getUserProfileDetail(@PathVariable Long userId) {
        // JWT 토큰의 subject에 guardianId가 들어있으므로 getName()으로 꺼내서 Long으로 변환
        Long guardianId = currentGuardianId(); // 팀원분이 짠 방식 그대로 활용!
        UserStateDetailResponse response = userService.getUserProfileDetail(userId, guardianId);
        return ResponseEntity.ok(response);
    }

    // 보호자Id 토큰으로 부터 가져오기
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
