package com.ikongserver.service;

import com.ikongserver.dto.EventDto.EmergencyAlertListResponse;
import com.ikongserver.dto.EventDto.EmergencyAlertResponse;
import com.ikongserver.dto.EventDto.EventSummaryResponse;
import com.ikongserver.dto.EventDto.ResponseEvent;
import com.ikongserver.dto.VitalDto.VitalRequestDto;
import com.ikongserver.entity.Device;
import com.ikongserver.entity.EmergencyEvent;
import com.ikongserver.entity.Guardian;
import com.ikongserver.entity.UserGuardianMap;
import com.ikongserver.entity.Users;
import com.ikongserver.repository.EmergencyEventRepository;
import com.ikongserver.repository.GuardianRepository;
import com.ikongserver.repository.UserGuardianMapRepository;
import com.ikongserver.repository.UsersRepository;
import java.util.List;
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

    // 낙상 감지시 프론트에 낙상 감지 보내기
    // 낙상 감지시 데이터를 Emergency_Event 테이블에 DB 저장 Type은 낙상
    public boolean checkFallEvent(VitalRequestDto vitalDto, Users user, Device device) {
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
    public boolean checkHeartBreathEvent(VitalRequestDto vitalDto, Users user,
        Device device) {

        boolean isIssueDetected = false;

        // 심박수 및 호흡 이상 감지
        // 심박수 120 이상 , 60 이하
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

        // 호흡수 30 이상 , 10 이하
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

    // 보호자 기준 해결건/미해결건 요약
    @Transactional(readOnly = true)
    public EventSummaryResponse getEventSummaryForGuardian(Long guardianId) {
        Guardian guardian = guardianRepository.findById(guardianId)
            .orElseThrow(() -> new IllegalArgumentException("보호자를 찾을 수 없습니다."));

        List<Users> users = userGuardianMapRepository.findByGuardian(guardian).stream()
            .map(UserGuardianMap::getUser)
            .toList();

        if (users.isEmpty()) return new EventSummaryResponse(0, 0);

        long unresolvedCount = emergencyEventRepository.countByUserInAndStatus(users, "PENDING");
        long resolvedCount = emergencyEventRepository.countByUserInAndStatus(users, "RESOLVED");

        return new EventSummaryResponse(resolvedCount, unresolvedCount);
    }

    // 보호자 기준 긴급 알림 목록
    @Transactional(readOnly = true)
    public EmergencyAlertListResponse getEmergencyAlertsForGuardian(Long guardianId) {
        Guardian guardian = guardianRepository.findById(guardianId)
            .orElseThrow(() -> new IllegalArgumentException("보호자를 찾을 수 없습니다."));

        List<Users> users = userGuardianMapRepository.findByGuardian(guardian).stream()
            .map(UserGuardianMap::getUser)
            .toList();

        if (users.isEmpty()) return new EmergencyAlertListResponse(List.of());

        List<EmergencyAlertResponse> alerts = emergencyEventRepository
            .findByUserInOrderByCreatedAtDesc(users).stream()
            .map(event -> new EmergencyAlertResponse(
                event.getId(),
                event.getUser().getName(),
                event.getEventType(),
                event.getStatus(),
                event.getCreatedAt(),
                toDetail(event.getEventType())
            ))
            .toList();

        return new EmergencyAlertListResponse(alerts);
    }

    // 개별 이벤트 해결 처리
    @Transactional
    public void resolveEvent(Long eventId) {
        EmergencyEvent event = emergencyEventRepository.findById(eventId)
            .orElseThrow(() -> new IllegalArgumentException("이벤트를 찾을 수 없습니다."));
        event.updateStatus("RESOLVED");
    }

    // 보호자 기준 전체 이벤트 해결 처리
    @Transactional
    public void resolveAllEvents(Long guardianId) {
        Guardian guardian = guardianRepository.findById(guardianId)
            .orElseThrow(() -> new IllegalArgumentException("보호자를 찾을 수 없습니다."));

        List<Users> users = userGuardianMapRepository.findByGuardian(guardian).stream()
            .map(UserGuardianMap::getUser)
            .toList();

        if (users.isEmpty()) return;

        emergencyEventRepository.findByUserInAndStatus(users, "PENDING")
            .forEach(event -> event.updateStatus("RESOLVED"));
    }

    private String toDetail(String eventType) {
        return switch (eventType) {
            case "FALL" -> "낙상이 감지되었습니다.";
            case "HEART_ISSUE" -> "심박수 이상이 감지되었습니다.";
            case "BREATH_ISSUE" -> "호흡수 이상이 감지되었습니다.";
            default -> "이상이 감지되었습니다.";
        };
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
