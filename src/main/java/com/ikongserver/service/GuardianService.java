package com.ikongserver.service;

import com.ikongserver.dto.GuardianDto;
import com.ikongserver.entity.Guardian;
import com.ikongserver.entity.GuardianInvitation;
import com.ikongserver.entity.UserGuardianMap;
import com.ikongserver.entity.Users;
import com.ikongserver.repository.GuardianInvitationRepository;
import com.ikongserver.repository.GuardianRepository;
import com.ikongserver.repository.UserGuardianMapRepository;
import com.ikongserver.repository.UsersRepository;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GuardianService {

    private static final int MAX_GUARDIAN_COUNT = 5;

    private final GuardianRepository guardianRepository;
    private final GuardianInvitationRepository guardianInvitationRepository;
    private final UserGuardianMapRepository userGuardianMapRepository;
    private final UsersRepository usersRepository;
    private final FcmService fcmService;

    // 보호자 직접 등록 — 최대 5명 제한 초과 시 예외 발생, Guardian + UserGuardianMap 동시 생성
    @Transactional
    public GuardianDto.ResponseRegister registerGuardian(Long userId, GuardianDto.RequestRegister request) {
        Users user = usersRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        long activeCount = userGuardianMapRepository.countByUserAndIsActive(user, "Y");
        if (activeCount >= MAX_GUARDIAN_COUNT) {
            throw new IllegalStateException("보호자는 최대 " + MAX_GUARDIAN_COUNT + "명까지 등록할 수 있습니다.");
        }

        Guardian guardian = Guardian.builder()
            .name(request.name())
            .phone(request.phone())
            .build();
        guardianRepository.save(guardian);

        String isPrimary = request.isPrimary() ? "Y" : "N";

        UserGuardianMap mapping = UserGuardianMap.builder()
            .user(user)
            .guardian(guardian)
            .relation(request.relation())
            .isPrimary(isPrimary)
            .isActive("Y")
            .build();
        userGuardianMapRepository.save(mapping);

        return new GuardianDto.ResponseRegister(
            guardian.getId(),
            guardian.getName(),
            guardian.getPhone(),
            mapping.getRelation(),
            "Y".equals(mapping.getIsPrimary()),
            "Y".equals(mapping.getIsActive())
        );
    }

    // 피보호자 ID로 등록된 보호자 목록 조회 (활성/비활성 포함)
    public List<GuardianDto.ResponseGuardian> getGuardians(Long userId) {
        Users user = usersRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        List<UserGuardianMap> mappings = userGuardianMapRepository.findByUser(user);

        return mappings.stream()
            .map(mapping -> new GuardianDto.ResponseGuardian(
                mapping.getGuardian().getId(),
                mapping.getGuardian().getName(),
                mapping.getGuardian().getPhone(),
                "Y".equals(mapping.getIsPrimary()),
                mapping.getRelation()
            ))
            .toList();
    }

    // 보호자 초대 발송 — GuardianInvitation 레코드 생성, 초대 수락 전까지 UserGuardianMap에 반영 안 됨
    @Transactional
    public GuardianDto.ResponseInvite inviteGuardian(Long userId, GuardianDto.RequestInvite request) {
        Users user = usersRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        GuardianInvitation invitation = GuardianInvitation.builder()
            .user(user)
            .phone(request.phone())
            .name(request.name())
            .relation(request.relation())
            .isPrimary(request.isPrimary())
            .build();
        guardianInvitationRepository.save(invitation);

        // 초대받은 보호자가 이미 앱에 가입된 경우 FCM 알림 발송
        guardianRepository.findByPhone(request.phone()).ifPresent(guardian -> {
            fcmService.sendPushNotification(
                guardian.getFcmToken(),
                "보호자 초대",
                user.getName() + "님이 보호자로 초대했습니다.",
                "ALERT"
            );
        });

        return new GuardianDto.ResponseInvite(
            invitation.getId(),
            invitation.getName(),
            invitation.getPhone(),
            invitation.getRelation(),
            invitation.getIsPrimary(),
            invitation.getStatus(),
            invitation.getCreatedAt()
        );
    }

    // 초대 수락 — GuardianInvitation status를 ACCEPTED로 변경, 피보호자에게 FCM 알림 발송
    @Transactional
    public void acceptInvitation(Long invitationId) {
        GuardianInvitation invitation = guardianInvitationRepository.findById(invitationId)
            .orElseThrow(() -> new IllegalArgumentException("초대를 찾을 수 없습니다."));
        invitation.accept();

        fcmService.sendPushNotification(
            invitation.getUser().getFcmToken(),
            "보호자 초대 수락",
            invitation.getName() + "님이 보호자 초대를 수락했습니다.",
            "ALERT"
        );
    }

    // 초대 거절 — GuardianInvitation status를 REJECTED로 변경, 피보호자에게 FCM 알림 발송
    @Transactional
    public void rejectInvitation(Long invitationId) {
        GuardianInvitation invitation = guardianInvitationRepository.findById(invitationId)
            .orElseThrow(() -> new IllegalArgumentException("초대를 찾을 수 없습니다."));
        invitation.reject();

        fcmService.sendPushNotification(
            invitation.getUser().getFcmToken(),
            "보호자 초대 거절",
            invitation.getName() + "님이 보호자 초대를 거절했습니다.",
            "ALERT"
        );
    }

    // 보호자 삭제 — 실제 레코드 삭제 대신 UserGuardianMap의 isActive를 "N"으로 변경 (소프트 삭제)
    @Transactional
    public void deleteGuardian(Long userId, Long guardianId) {
        Users user = usersRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        UserGuardianMap mapping = userGuardianMapRepository.findByUser(user).stream()
            .filter(m -> m.getGuardian().getId().equals(guardianId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("해당 보호자를 찾을 수 없습니다."));

        mapping.updateIsActive("N");
    }
}
