package com.ikongserver.service;

import com.ikongserver.dto.UserDto;
import com.ikongserver.entity.Users;
import com.ikongserver.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public UserDto.MainProfileResponse getMainProfile(Long userId) {

        // 피보호자 아이디 조회 및 예외 처리
        Users user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("해당 사용자를 찾을 수 없습니다. ID: " + userId));

        // TODO: vital에서 수치에서 이상 감지 시 상태 변환 로직 필요
        // 임시 데이터

        String currentStatus = "정상";
        return new UserDto.MainProfileResponse(
            user.getId(),
            user.getName(),
            currentStatus
        );
    }
}
