package com.ikongserver.service;

import com.ikongserver.dto.EventDto;
import com.ikongserver.dto.VitalDto;
import com.ikongserver.entity.Device;
import com.ikongserver.entity.EmergencyEvent;
import com.ikongserver.entity.Users;
import com.ikongserver.repository.EmergencyEventRepository;
import com.ikongserver.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmergencyEventService {

    private final EmergencyEventRepository emergencyEventRepository;
    private final UsersRepository userRepository;

    // 낙상 감지시 프론트에 낙상 감지 보내기
    // 낙상 감지시 데이터를 Emergency_Event 테이블에 DB 저장 Type은 낙상
    public boolean checkFallEvent(VitalDto.VitalRequestDto vitalDto, Users user, Device device) {
        // 낙상 감지
        if (vitalDto.isFallDetected()) {
            EmergencyEvent fallEvent = EmergencyEvent.builder()
                .user(user)
                .eventType("FALL")
                .status("PENDING")
                .device(device)
                .build();
            emergencyEventRepository.save(fallEvent);
            return true;

        }
        return false;
    }

    // 심박수 및 호흡 이상감지 시 프론트에 심장수 및 호흡이상 감지 보내기
    // 심박 및 호흡 이상 감지시 데이터를 Emergency_Event 테이블에 DB 저장 type은 심박수 및 호흡이상 감지
    public boolean checkHeartBreathEvent(VitalDto.VitalRequestDto vitalDto, Users user,
        Device device) {

        boolean isIssueDetected = false;

        // 심박수 및 호흡 이상 감지
        if (vitalDto.heartRate() >= 120 || vitalDto.heartRate() <= 60) {
            EmergencyEvent heartEvent = EmergencyEvent.builder()
                .user(user)
                .eventType("HEART_ISSUE")
                .status("PENDING")
                .device(device)
                .build();
            emergencyEventRepository.save(heartEvent);
            isIssueDetected = true;

        }

        if (vitalDto.breathRate() >= 30 || vitalDto.breathRate() <= 10) {
            EmergencyEvent breathEvent = EmergencyEvent.builder()
                .user(user)
                .eventType("BREATH_ISSUE")
                .status("PENDING")
                .device(device)
                .build();
            emergencyEventRepository.save(breathEvent);
            isIssueDetected = true;
        }

        return isIssueDetected;
    }

    @Transactional(readOnly = true)
    public EventDto.ResponseEvent getLatestPendingEvent(long userId) {
        // 1. 유저 조회
        Users user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 2. 해당 유저의 이벤트 중 PENDING(미해결) 상태인 것을 최신순으로 1개만 조회
        // (Repository에 findTopByUserAndStatusOrderByCreatedAtDesc 메서드 필요)
        return emergencyEventRepository.findTopByUserAndStatusOrderByCreatedAtDesc(user, "PENDING")
            .map(event -> new EventDto.ResponseEvent(
                event.getId(),
                event.getEventType(), // "FALL", "HEART_ISSUE" 등
                event.getStatus(),
                event.getCreatedAt()
            ))
            .orElse(null); // 없으면 null 반환 (프론트에서 배너를 안 띄움)
    }


}
