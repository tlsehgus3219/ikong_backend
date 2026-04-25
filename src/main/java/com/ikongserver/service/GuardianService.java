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
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GuardianService {

    private static final int MAX_GUARDIAN_COUNT = 5;

    private final GuardianRepository guardianRepository;
    private final GuardianInvitationRepository guardianInvitationRepository;
    private final UserGuardianMapRepository userGuardianMapRepository;
    private final UsersRepository usersRepository;

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
