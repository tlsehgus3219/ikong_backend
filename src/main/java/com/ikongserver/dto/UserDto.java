package com.ikongserver.dto;

import java.time.LocalDateTime;

public class UserDto {

    // 피보호자 (이름, 수치(정상, 비정상, 이상))
    public record MainProfileResponse(Long id, String name, String status) {

    }

    public record FcmTokenRequest(String fcmToken) {
    }

    // 보호자 화면에서의 피보호자 상태 디테일 (이름, 관계, 상태, 실시간 생체 데이터(심박수, 호흡수, 최근 업데이트 시간), 활동 상태 (수면, 낙상, 활동 중))
    public record UserStateDetailResponse(Long userId, String name, String relationship,
                                          String overallStatus, int heartRate,
                                          int breathRate, LocalDateTime updatedAt,
                                          boolean activityStatus) {

    }

}
