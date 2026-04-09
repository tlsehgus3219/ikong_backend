package com.ikongserver.auth.service;

import com.ikongserver.dto.AuthDto.KakaoLoginRequest;
import com.ikongserver.dto.AuthDto.LoginResponse;
import com.ikongserver.dto.AuthDto.LogoutRequest;
import com.ikongserver.dto.AuthDto.RefreshRequest;
import com.ikongserver.dto.AuthDto.RefreshResponse;
import com.ikongserver.dto.AuthDto.SignupRequest;
import com.ikongserver.dto.AuthDto.SignupResponse;
import com.ikongserver.entity.Guardian;
import com.ikongserver.entity.LogoutToken;
import com.ikongserver.entity.Users;
import com.ikongserver.jwt.JwtUtil;
import com.ikongserver.repository.GuardianRepository;
import com.ikongserver.repository.LogoutTokenRepository;
import com.ikongserver.repository.UsersRepository;
import io.jsonwebtoken.Claims;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final KakaoAuthService kakaoAuthService;
    private final UsersRepository usersRepository;
    private final GuardianRepository guardianRepository;
    private final LogoutTokenRepository logoutTokenRepository;
    private final JwtUtil jwtUtil;

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        if (usersRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("이미 사용 중인 이메일입니다.");
        }

        Users user = usersRepository.save(Users.builder()
                .email(request.getEmail())
                .password(request.getPassword())
                .name(request.getName())
                .phone(request.getPhone())
                .birthDate(request.getBirthDate())
                .build());

        return SignupResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .build();
    }

    @Transactional
    public LoginResponse kakaoLogin(KakaoLoginRequest request) {
        String socialId = kakaoAuthService.getKakaoSocialId(request.getKakaoAccessToken());
        String userType = request.getUserType();

        boolean isNewUser = false;
        String id;

        if ("GUARDIAN".equals(userType)) {
            boolean[] isNew = {false};
            Guardian guardian = guardianRepository.findBySocialId(socialId)
                    .orElseGet(() -> {
                        isNew[0] = true;
                        return guardianRepository.save(Guardian.builder()
                                .socialId(socialId)
                                .name("보호자")
                                .build());
                    });
            id = String.valueOf(guardian.getId());
            isNewUser = isNew[0];
        } else {
            boolean[] isNew = {false};
            Users user = usersRepository.findBySocialId(socialId)
                    .orElseGet(() -> {
                        isNew[0] = true;
                        return usersRepository.save(Users.builder()
                                .socialId(socialId)
                                .name("사용자")
                                .build());
                    });
            id = String.valueOf(user.getId());
            isNewUser = isNew[0];
        }

        String accessToken = jwtUtil.generateAccessToken(id, userType);
        String refreshToken = jwtUtil.generateRefreshToken(id, userType);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(3600)
                .isNewUser(isNewUser)
                .build();
    }

    @Transactional
    public RefreshResponse refresh(RefreshRequest request) {
        String refreshToken = request.getRefreshToken();

        if (!jwtUtil.isTokenValid(refreshToken)) {
            throw new RuntimeException("유효하지 않은 Refresh Token입니다.");
        }

        if (logoutTokenRepository.existsByToken(refreshToken)) {
            throw new RuntimeException("이미 로그아웃된 토큰입니다.");
        }

        Claims claims = jwtUtil.parseToken(refreshToken);
        String newAccessToken = jwtUtil.generateAccessToken(claims.getSubject(), claims.get("userType", String.class));

        return RefreshResponse.builder()
                .accessToken(newAccessToken)
                .expiresIn(3600)
                .build();
    }

    @Transactional
    public void logout(LogoutRequest request) {
        String refreshToken = request.getRefreshToken();

        if (!jwtUtil.isTokenValid(refreshToken)) {
            throw new RuntimeException("유효하지 않은 Refresh Token입니다.");
        }

        Claims claims = jwtUtil.parseToken(refreshToken);
        LocalDateTime expiredAt = LocalDateTime.ofInstant(
                claims.getExpiration().toInstant(),
                java.time.ZoneId.systemDefault()
        );

        logoutTokenRepository.save(LogoutToken.builder()
                .token(refreshToken)
                .expiredAt(expiredAt)
                .build());
    }
}
