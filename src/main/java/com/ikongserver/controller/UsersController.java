package com.ikongserver.controller;

import com.ikongserver.dto.UserDto.FcmTokenRequest;
import com.ikongserver.dto.UserDto.MainProfileResponse;
import com.ikongserver.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UsersController {

    private final UserService userService;

    // 메인 화면 이름 및 상태 표시
    @GetMapping("/{userId}/main")
    public ResponseEntity<MainProfileResponse> getUser(@PathVariable Long userId) {
        MainProfileResponse response = userService.getMainProfile(userId);
        return ResponseEntity.ok(response);
    }

    // 피보호자 FCM 토큰 등록/갱신 — 앱 실행 시 최신 토큰을 서버에 저장
    @PutMapping("/fcm-token")
    public ResponseEntity<Void> updateFcmToken(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody FcmTokenRequest request) {
        Long userId = Long.parseLong(userDetails.getUsername());
        userService.updateFcmToken(userId, request.fcmToken());
        return ResponseEntity.ok().build();
    }
}
