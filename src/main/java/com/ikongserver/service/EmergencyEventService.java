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

    // 낙상 감지 여부 확인 — isFallDetected=true면 FALL 이벤트 저장 후 true 반환 (VitalService에서 호출)
    public boolean checkFallEvent(VitalRequestDto vitalDto, Users user, Device device) {
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

    // 심박수(60~120 정상) / 호흡수(10~30 정상) 이상 여부 확인 — 범위 초과 시 각각 이벤트 저장
    public boolean checkHeartBreathEvent(VitalRequestDto vitalDto, Users user, Device device) {
        boolean isIssueDetected = false;

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

    // 보호자 ID로 담당 피보호자 전체의 해결/미해결 이벤트 수를 집계하여 반환
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

    // 보호자 ID로 담당 피보호자 전체의 응급 이벤트 목록을 최신순으로 조회 (이벤트 타입별 상세 설명 포함)
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
                event.getEventDescription())
            ).toList();

        return new EmergencyAlertListResponse(alerts);
    }

    // 특정 이벤트를 RESOLVED로 변경 — 해당 보호자가 담당하는 피보호자의 이벤트인지 권한 검증 후 처리
    @Transactional
    public ResponseEvent resolveEvent(Long guardianId, Long eventId) {
        Guardian guardian = guardianRepository.findById(guardianId)
            .orElseThrow(() -> new IllegalArgumentException("보호자를 찾을 수 없습니다."));

        EmergencyEvent event = emergencyEventRepository.findById(eventId)
            .orElseThrow(() -> new IllegalArgumentException("응급 이벤트를 찾을 수 없습니다."));

        boolean authorized = userGuardianMapRepository
            .findByGuardianAndIsActive(guardian, "Y").stream()
            .anyMatch(m -> m.getUser().getId().equals(event.getUser().getId()));

        if (!authorized) {
            throw new IllegalStateException("해당 이벤트를 해결할 권한이 없습니다.");
        }

        if (!"RESOLVED".equals(event.getStatus())) {
            event.updateStatus("RESOLVED");
        }

        return new ResponseEvent(event.getId(), event.getEventType(), event.getStatus(), event.getCreatedAt());
    }

    // 보호자 ID로 담당 피보호자 전체의 PENDING 이벤트를 한 번에 RESOLVED로 일괄 처리
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

    // 피보호자 ID로 가장 최근 PENDING 이벤트 1건 조회 — 없으면 null 반환 (앱 팝업 표시 여부 판단용)
    @Transactional(readOnly = true)
    public ResponseEvent getLatestPendingEvent(long userId) {
        Users user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        return emergencyEventRepository.findTopByUserAndStatusOrderByCreatedAtDesc(user, "PENDING")
            .map(event -> new ResponseEvent(
                event.getId(),
                event.getEventType(),
                event.getStatus(),
                event.getCreatedAt()
            ))
            .orElse(null);
    }
}
