package com.ikongserver.dto;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

public class AuthDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SignupRequest {
        private String email;
        private String password;
        private String name;
        private String phone;
        private LocalDate birthDate; // 선택
    }

    @Getter
    @Builder
    public static class SignupResponse {
        private Long userId;
        private String email;
        private String name;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KakaoLoginRequest {
        private String kakaoAccessToken;
        private String userType; // USER | GUARDIAN
    }

    @Getter
    @Builder
    public static class LoginResponse {
        private String accessToken;
        private String refreshToken;
        private String tokenType;
        private long expiresIn;
        private boolean isNewUser;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefreshRequest {
        private String refreshToken;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LogoutRequest {
        private String refreshToken;
    }

    @Getter
    @Builder
    public static class RefreshResponse {
        private String accessToken;
        private long expiresIn;
    }
}
