package com.ikongserver.dto;

import java.time.LocalDate;

public class AuthDto {

    public record SignupRequest(String email, String password, String name, String phone, LocalDate birthDate) {}

    public record SignupResponse(Long userId, String email, String name) {}

    public record KakaoLoginRequest(String kakaoAccessToken, String userType) {}

    public record LoginResponse(String accessToken, String refreshToken, String tokenType, long expiresIn, boolean isNewUser) {}

    public record RefreshRequest(String refreshToken) {}

    public record LogoutRequest(String refreshToken) {}

    public record RefreshResponse(String accessToken, long expiresIn) {}
}
