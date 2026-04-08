package com.ikongserver.auth.controller;

import com.ikongserver.auth.service.AuthService;
import com.ikongserver.dto.AuthDto.KakaoLoginRequest;
import com.ikongserver.dto.AuthDto.LoginResponse;
import com.ikongserver.dto.AuthDto.LogoutRequest;
import com.ikongserver.dto.AuthDto.RefreshRequest;
import com.ikongserver.dto.AuthDto.RefreshResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> kakaoLogin(@RequestBody KakaoLoginRequest request) {
        return ResponseEntity.ok(authService.kakaoLogin(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<RefreshResponse> refresh(@RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestBody LogoutRequest request) {
        authService.logout(request);
        return ResponseEntity.ok("로그아웃되었습니다.");
    }
}
