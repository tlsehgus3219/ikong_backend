package com.ikongserver.service;

import com.ikongserver.dto.UserDto;
import com.ikongserver.entity.Users;
import com.ikongserver.repository.EmergencyEventRepository;
import com.ikongserver.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final EmergencyEventRepository emergencyEventRepository;

    public UserDto.MainProfileResponse getMainProfile(Long userId) {

        // 피보호자 아이디 조회 및 예외 처리
        Users user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("해당 사용자를 찾을 수 없습니다. ID: " + userId));

        // 이상 감지 파악
        boolean hasEmergency = emergencyEventRepository.existsByUserAndStatus(user, "PENDING");

        String currentStatus = hasEmergency ? "위험" : "정상";

        return new UserDto.MainProfileResponse(
            user.getId(),
            user.getName(),
            currentStatus
        );
    }
}
