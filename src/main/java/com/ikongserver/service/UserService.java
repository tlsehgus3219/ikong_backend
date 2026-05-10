package com.ikongserver.service;

import com.ikongserver.dto.UserDto.MainProfileResponse;
import com.ikongserver.dto.UserDto.UserStateDetailResponse;
import com.ikongserver.entity.UserGuardianMap;
import com.ikongserver.entity.Users;
import com.ikongserver.entity.Vital;
import com.ikongserver.repository.EmergencyEventRepository;
import com.ikongserver.repository.UserGuardianMapRepository;
import com.ikongserver.repository.UsersRepository;
import com.ikongserver.repository.VitalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UsersRepository userRepository;
    private final EmergencyEventRepository emergencyEventRepository;
    private final UserGuardianMapRepository userGuardianMapRepository;
    private final VitalRepository vitalRepository;

    public MainProfileResponse getMainProfile(Long userId) {

        // 피보호자 아이디 조회 및 예외 처리
        Users user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("해당 사용자를 찾을 수 없습니다. ID: " + userId));

        // 이상 감지 파악
        boolean hasEmergency = emergencyEventRepository.existsByUserAndStatus(user, "PENDING");

        String currentStatus = hasEmergency ? "위험" : "정상";

        return new MainProfileResponse(
            user.getId(),
            user.getName(),
            currentStatus
        );
    }

    @Transactional(readOnly = true)
    public UserStateDetailResponse getUserProfileDetail(Long userId, Long guardianId) {

        // 관계 검증
        UserGuardianMap isLinked = userGuardianMapRepository.findByUserIdAndGuardianId(userId,
            guardianId).orElseThrow(() -> new RuntimeException("권한이 없는 피보호자 입니다."));

        // 피보호자 정보 가져오기
        Users user = isLinked.getUser();

        // 최근 생체 정보 가져오기
        Vital latestVital = vitalRepository.findFirstByUserIdOrderByRecordedAtDesc(userId)
            .orElse(null);

        // 심장, 호흡 값 확인 true(정상), false(긴급)
        boolean heartBreathRateCheck = true;
        // 낙상 값 확인
        boolean activityStatus = true;

        if (latestVital != null) {
            // Vital 정상값 비정상값 판별
            heartBreathRateCheck = latestVital.isHeartBreathNormal();
            // 낙상 감지가 true(발생)면, activityStatus(활동상태)는 false(비정상)가 됩니다.
            activityStatus = !latestVital.isFallDetected();
        }

        String overallStatus = (heartBreathRateCheck && activityStatus) ? "정상" : "긴급";

        // dto로 변환
        return new UserStateDetailResponse(user.getId(), user.getName(), isLinked.getRelation(),
            overallStatus, latestVital != null ? latestVital.getHeartRate() : 0,
            latestVital != null ? latestVital.getBreathRate() : 0,
            latestVital != null ? latestVital.getRecordedAt() : null, activityStatus);
    }
}
