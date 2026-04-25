package com.ikongserver.service;

import com.ikongserver.dto.AuthDto.KakaoLoginRequest;
import com.ikongserver.dto.AuthDto.LoginResponse;
import com.ikongserver.dto.AuthDto.LogoutRequest;
import com.ikongserver.dto.AuthDto.RefreshRequest;
import com.ikongserver.dto.AuthDto.RefreshResponse;
import com.ikongserver.dto.AuthDto.SignupRequest;
import com.ikongserver.dto.AuthDto.SignupResponse;
import com.ikongserver.entity.Guardian;
import com.ikongserver.entity.GuardianInvitation;
import com.ikongserver.entity.LogoutToken;
import com.ikongserver.entity.UserGuardianMap;
import com.ikongserver.entity.Users;
import com.ikongserver.jwt.JwtUtil;
import com.ikongserver.repository.GuardianInvitationRepository;
import com.ikongserver.repository.GuardianRepository;
import com.ikongserver.repository.LogoutTokenRepository;
import com.ikongserver.repository.UserGuardianMapRepository;
import com.ikongserver.repository.UsersRepository;
import io.jsonwebtoken.Claims;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int MAX_GUARDIAN_COUNT = 5;

    private final KakaoAuthService kakaoAuthService;
    private final UsersRepository usersRepository;
    private final GuardianRepository guardianRepository;
    private final GuardianInvitationRepository guardianInvitationRepository;
    private final UserGuardianMapRepository userGuardianMapRepository;
    private final LogoutTokenRepository logoutTokenRepository;
    private final JwtUtil jwtUtil;

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        if (usersRepository.existsByEmail(request.email())) {
            throw new RuntimeException("이미 사용 중인 이메일입니다.");
        }

        Users user = usersRepository.save(Users.builder()
                .email(request.email())
                .password(request.password())
                .name(request.name())
                .phone(request.phone())
                .birthDate(request.birthDate())
                .build());

        return new SignupResponse(user.getId(), user.getEmail(), user.getName());
    }

    @Transactional
    public LoginResponse kakaoLogin(KakaoLoginRequest request) {
        KakaoAuthService.KakaoUserInfo kakaoUser = kakaoAuthService.getKakaoUserInfo(request.kakaoAccessToken());
        String socialId = kakaoUser.socialId();
        String userType = request.userType();

        boolean isNewUser = false;
        String id;

        if ("GUARDIAN".equals(userType)) {
            boolean[] isNew = {false};
            Guardian guardian = guardianRepository.findBySocialId(socialId)
                    .orElseGet(() -> {
                        isNew[0] = true;
                        return guardianRepository.save(Guardian.builder()
                                .socialId(socialId)
                                .name(kakaoUser.name() != null ? kakaoUser.name() : "보호자")
                                .phone(kakaoUser.phone())
                                .build());
                    });

            // 전화번호로 PENDING 초대 확인 및 자동 수락 (신규/기존 보호자 모두)
            if (kakaoUser.phone() != null) {
                List<GuardianInvitation> pendingInvitations =
                        guardianInvitationRepository.findByPhoneAndStatus(kakaoUser.phone(), "PENDING");

                for (GuardianInvitation invitation : pendingInvitations) {
                    Users invitingUser = invitation.getUser();
                    boolean alreadyMapped = userGuardianMapRepository
                            .existsByUserAndGuardian(invitingUser, guardian);
                    if (!alreadyMapped) {
                        long activeCount = userGuardianMapRepository
                                .countByUserAndIsActive(invitingUser, "Y");
                        if (activeCount < MAX_GUARDIAN_COUNT) {
                            userGuardianMapRepository.save(UserGuardianMap.builder()
                                    .user(invitingUser)
                                    .guardian(guardian)
                                    .relation(invitation.getRelation())
                                    .isPrimary(invitation.getIsPrimary() ? "Y" : "N")
                                    .isActive("Y")
                                    .build());
                        }
                    }
                    invitation.accept();
                }
            }

            id = String.valueOf(guardian.getId());
            isNewUser = isNew[0];
        } else {
            boolean[] isNew = {false};
            Users user = usersRepository.findBySocialId(socialId)
                    .orElseGet(() -> {
                        isNew[0] = true;
                        return usersRepository.save(Users.builder()
                                .socialId(socialId)
                                .name(kakaoUser.name() != null ? kakaoUser.name() : "사용자")
                                .phone(kakaoUser.phone())
                                .birthDate(kakaoUser.birthDate())
                                .build());
                    });
            // 기존 유저 카카오 최신 정보로 업데이트
            if (!isNew[0]) {
                user.updateFromKakao(kakaoUser.name(), kakaoUser.phone(), kakaoUser.birthDate());
            }
            id = String.valueOf(user.getId());
            isNewUser = isNew[0];
        }

        String accessToken = jwtUtil.generateAccessToken(id, userType);
        String refreshToken = jwtUtil.generateRefreshToken(id, userType);

        return new LoginResponse(accessToken, refreshToken, "Bearer", 3600, isNewUser);
    }

    @Transactional
    public RefreshResponse refresh(RefreshRequest request) {
        String refreshToken = request.refreshToken();

        if (!jwtUtil.isTokenValid(refreshToken)) {
            throw new RuntimeException("유효하지 않은 Refresh Token입니다.");
        }

        if (logoutTokenRepository.existsByToken(refreshToken)) {
            throw new RuntimeException("이미 로그아웃된 토큰입니다.");
        }

        Claims claims = jwtUtil.parseToken(refreshToken);
        String newAccessToken = jwtUtil.generateAccessToken(claims.getSubject(), claims.get("userType", String.class));

        return new RefreshResponse(newAccessToken, 3600);
    }

    @Transactional
    public void logout(LogoutRequest request) {
        String refreshToken = request.refreshToken();

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
