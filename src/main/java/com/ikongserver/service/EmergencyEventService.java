package com.ikongserver.service;

import com.ikongserver.dto.EventDto.ResponseEvent;
import com.ikongserver.dto.VitalDto.VitalRequestDto;
import com.ikongserver.entity.Device;
import com.ikongserver.entity.EmergencyEvent;
import com.ikongserver.entity.Guardian;
import com.ikongserver.entity.Users;
import com.ikongserver.repository.EmergencyEventRepository;
import com.ikongserver.repository.GuardianRepository;
import com.ikongserver.repository.UserGuardianMapRepository;
import com.ikongserver.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmergencyEventService {

    private final EmergencyEventRepository emergencyEventRepository;
    private final UsersRepository userRepository;
    private final GuardianRepository guardianRepository;
    private final UserGuardianMapRepository userGuardianMapRepository;
    private final NotificationService notificationService;

    // 낙상 감지시 프론트에 낙상 감지 보내기
    // 낙상 감지시 데이터를 Emergency_Event 테이블에 DB 저장 Type은 낙상
    public boolean checkFallEvent(VitalRequestDto vitalDto, Users user, Device device) {
        if (!vitalDto.isFallDetected()) {
            return false;
        }
        // 중복 방지: 이미 PENDING 낙상 이벤트가 있으면 새로 만들지 않음
        if (emergencyEventRepository.existsByUserAndEventTypeAndStatus(user, "FALL", "PENDING")) {
            return false;
        }

        EmergencyEvent fallEvent = EmergencyEvent.builder()
            .user(user)
            .eventType("FALL")
            .status("PENDING")
            .device(device)
            .build();
        emergencyEventRepository.save(fallEvent);

        // 활성 보호자 전원에게 알림 자동 생성 (1차 알림)
        notificationService.createForEvent(fallEvent, "낙상이 감지되었습니다");
        return true;
    }

    // 심박수 및 호흡 이상감지 시 프론트에 심장수 및 호흡이상 감지 보내기
    // 심박 및 호흡 이상 감지시 데이터를 Emergency_Event 테이블에 DB 저장 type은 심박수 및 호흡이상 감지
    public boolean checkHeartBreathEvent(VitalRequestDto vitalDto, Users user,
        Device device) {

        boolean isIssueDetected = false;

        // 심박수 120 이상 또는 60 이하 (이미 PENDING 있으면 건너뜀)
        if (vitalDto.heartRate() >= 120 || vitalDto.heartRate() <= 60) {
            if (!emergencyEventRepository.existsByUserAndEventTypeAndStatus(user, "HEART_ISSUE", "PENDING")) {
                EmergencyEvent heartEvent = EmergencyEvent.builder()
                    .user(user)
                    .eventType("HEART_ISSUE")
                    .status("PENDING")
                    .device(device)
                    .build();
                emergencyEventRepository.save(heartEvent);

                String msg = String.format("심박수 이상이 감지되었습니다 (현재 %dbpm)", vitalDto.heartRate());
                notificationService.createForEvent(heartEvent, msg);
                isIssueDetected = true;
            }
        }

        // 호흡수 30 이상 또는 10 이하 (이미 PENDING 있으면 건너뜀)
        if (vitalDto.breathRate() >= 30 || vitalDto.breathRate() <= 10) {
            if (!emergencyEventRepository.existsByUserAndEventTypeAndStatus(user, "BREATH_ISSUE", "PENDING")) {
                EmergencyEvent breathEvent = EmergencyEvent.builder()
                    .user(user)
                    .eventType("BREATH_ISSUE")
                    .status("PENDING")
                    .device(device)
                    .build();
                emergencyEventRepository.save(breathEvent);

                String msg = String.format("호흡 이상이 감지되었습니다 (현재 %d회/분)", vitalDto.breathRate());
                notificationService.createForEvent(breathEvent, msg);
                isIssueDetected = true;
            }
        }

        return isIssueDetected;
    }

    /**
     * 보호자가 응급 이벤트 1건을 해결 처리.
     * - 본인이 담당하는 피보호자의 이벤트만 해결 가능
     * - 이미 RESOLVED 상태인 경우 멱등하게 그대로 반환
     */
    @Transactional
    public ResponseEvent resolveEvent(Long guardianId, Long eventId) {
        Guardian guardian = guardianRepository.findById(guardianId)
            .orElseThrow(() -> new IllegalArgumentException("보호자를 찾을 수 없습니다."));

        EmergencyEvent event = emergencyEventRepository.findById(eventId)
            .orElseThrow(() -> new IllegalArgumentException("응급 이벤트를 찾을 수 없습니다."));

        // 권한 체크: 보호자가 해당 피보호자를 담당하고 있는지 확인
        boolean authorized = userGuardianMapRepository
            .findByGuardianAndIsActive(guardian, "Y").stream()
            .anyMatch(m -> m.getUser().getId().equals(event.getUser().getId()));

        if (!authorized) {
            throw new IllegalStateException("해당 이벤트를 해결할 권한이 없습니다.");
        }

        if (!"RESOLVED".equals(event.getStatus())) {
            event.updateStatus("RESOLVED");
        }

        return new ResponseEvent(
            event.getId(),
            event.getEventType(),
            event.getStatus(),
            event.getCreatedAt()
        );
    }

    // 응급 상황 중 미해결 조회
    @Transactional(readOnly = true)
    public ResponseEvent getLatestPendingEvent(long userId) {
        // 유저 조회
        Users user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 해당 유저의 이벤트 중 PENDING(미해결) 상태인 것을 최신순으로 1개만 조회
        return emergencyEventRepository.findTopByUserAndStatusOrderByCreatedAtDesc(user, "PENDING")
            .map(event -> new ResponseEvent(
                event.getId(),
                event.getEventType(), // "FALL", "HEART_ISSUE" 등
                event.getStatus(),
                event.getCreatedAt()
            ))
            .orElse(null); // 없으면 null 반환 (프론트에서 배너를 안 띄움)
    }

}
