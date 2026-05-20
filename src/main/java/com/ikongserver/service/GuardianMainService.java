package com.ikongserver.service;

import com.ikongserver.dto.GuardianMainDto.DashboardSummary;
import com.ikongserver.dto.GuardianMainDto.LatestEvent;
import com.ikongserver.dto.GuardianMainDto.UserCard;
import com.ikongserver.dto.GuardianMainDto.UserListResponse;
import com.ikongserver.entity.Device;
import com.ikongserver.entity.EmergencyEvent;
import com.ikongserver.entity.Guardian;
import com.ikongserver.entity.UserGuardianMap;
import com.ikongserver.entity.Users;
import com.ikongserver.entity.Vital;
import com.ikongserver.repository.DeviceRepository;
import com.ikongserver.repository.EmergencyEventRepository;
import com.ikongserver.repository.GuardianRepository;
import com.ikongserver.repository.UserGuardianMapRepository;
import com.ikongserver.repository.VitalRepository;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 보호자 메인 화면용 서비스.
 * - 담당하는 피보호자 목록 조회
 * - 상단 카운트(긴급/주의/외출) 요약
 * - 멀티 센서 기반 상태 판정 (긴급 / 외출 / 미연결 / 정상)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GuardianMainService {

    // 미연결 / 외출 판단 기준 시간 (분)
    private static final long OFFLINE_THRESHOLD_MINUTES = 1;
    private static final long AWAY_THRESHOLD_MINUTES = 1;

    private final GuardianRepository guardianRepository;
    private final UserGuardianMapRepository userGuardianMapRepository;
    private final DeviceRepository deviceRepository;
    private final VitalRepository vitalRepository;
    private final EmergencyEventRepository emergencyEventRepository;

    /**
     * 보호자가 담당하는 피보호자 카드 목록 조회.
     */
    public UserListResponse getMyUsers(Long guardianId) {
        Guardian guardian = guardianRepository.findById(guardianId)
            .orElseThrow(() -> new IllegalArgumentException("보호자를 찾을 수 없습니다."));

        List<UserGuardianMap> mappings =
            userGuardianMapRepository.findByGuardianAndIsActive(guardian, "Y");

        // 긴급 상태인 피보호자가 위로 올라오도록 정렬 (그 외는 등록 순서 유지)
        List<UserCard> cards = mappings.stream()
            .map(this::buildUserCard)
            .sorted((a, b) -> {
                int pa = "EMERGENCY".equals(a.status()) ? 0 : 1;
                int pb = "EMERGENCY".equals(b.status()) ? 0 : 1;
                return Integer.compare(pa, pb);
            })
            .toList();

        return new UserListResponse(cards.size(), cards);
    }

    /**
     * 상단 카운트(대시보드) 조회.
     * - totalCount    : 모니터링 중인 피보호자 수
     * - emergencyCount: 미처리(PENDING) 긴급 알림 총 건수 (한 명에게 여러 건이면 모두 합산)
     * - awayCount     : 외출 상태 피보호자 수
     * - warningCount  : 보류 (현재 0)
     */
    public DashboardSummary getDashboard(Long guardianId) {
        Guardian guardian = guardianRepository.findById(guardianId)
            .orElseThrow(() -> new IllegalArgumentException("보호자를 찾을 수 없습니다."));

        List<UserGuardianMap> mappings =
            userGuardianMapRepository.findByGuardianAndIsActive(guardian, "Y");

        // 담당 피보호자 전체에 대해 PENDING 이벤트 개수 합산
        long emergencyCount = mappings.stream()
            .mapToLong(m -> emergencyEventRepository.countByUserAndStatus(m.getUser(), "PENDING"))
            .sum();

        // 외출 상태 카운트는 카드 상태 판정 결과 활용
        long awayCount = mappings.stream()
            .map(this::buildUserCard)
            .filter(c -> "AWAY".equals(c.status()))
            .count();

        return new DashboardSummary(emergencyCount, 0, awayCount, mappings.size());
    }

    /**
     * 특정 피보호자 상세 조회 (긴급 상황 UI용).
     */
    public UserCard getUserDetail(Long guardianId, Long userId) {
        Guardian guardian = guardianRepository.findById(guardianId)
            .orElseThrow(() -> new IllegalArgumentException("보호자를 찾을 수 없습니다."));

        UserGuardianMap mapping = userGuardianMapRepository
            .findByGuardianAndIsActive(guardian, "Y").stream()
            .filter(m -> m.getUser().getId().equals(userId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("담당하는 피보호자가 아닙니다."));

        return buildUserCard(mapping);
    }

    /**
     * UserGuardianMap 하나로부터 UserCard 생성.
     * 상태 판정 우선순위: 긴급 > 미연결 > 외출 > 정상
     */
    private UserCard buildUserCard(UserGuardianMap mapping) {
        Users user = mapping.getUser();

        // 1. 디바이스 / 최신 vital / 미해결 이벤트 한 번씩만 조회
        List<Device> devices = deviceRepository.findAllByUserId(user.getId());
        List<Vital> latestVitals = vitalRepository.findLatestVitalPerDevice(user.getId());
        Optional<EmergencyEvent> pendingEvent =
            emergencyEventRepository.findTopByUserAndStatusOrderByCreatedAtDesc(user, "PENDING");

        // 2. 상태 판정
        String status = determineStatus(devices, latestVitals, pendingEvent.isPresent());

        // 3. 화면에 보여줄 BPM 결정 — 낙상 센서(heartRate=0)는 제외하고 최신 값 표시
        Integer heartRate = null;
        Integer breathRate = null;
        if (!latestVitals.isEmpty()) {
            Vital newest = latestVitals.stream()
                .filter(v -> v.getHeartRate() > 0)
                .max((a, b) -> a.getRecordedAt().compareTo(b.getRecordedAt()))
                .orElse(null);
            if (newest != null) {
                heartRate = newest.getHeartRate();
                breathRate = newest.getBreathRate();
            }
        }

        // 4. 최근 응급 이벤트
        LatestEvent latestEvent = pendingEvent
            .map(e -> new LatestEvent(e.getId(), e.getEventType(), e.getStatus(), e.getCreatedAt()))
            .orElse(null);

        return new UserCard(
            user.getId(),
            user.getName(),
            user.getPhone(),
            mapping.getRelation(),
            "Y".equals(mapping.getIsPrimary()),
            status,
            heartRate,
            breathRate,
            latestEvent
        );
    }

    /**
     * 상태 판정.
     * 우선순위: 긴급(EMERGENCY) > 미연결(OFFLINE) > 외출(AWAY) > 정상(NORMAL)
     */
    private String determineStatus(List<Device> devices, List<Vital> latestVitals,
        boolean hasPendingEvent) {

        // 1. 긴급
        if (hasPendingEvent) {
            return "EMERGENCY";
        }

        // 디바이스가 한 대도 없으면 미연결로 간주
        if (devices.isEmpty()) {
            return "OFFLINE";
        }

        LocalDateTime now = LocalDateTime.now();

        // 2. 미연결: 모든 디바이스의 last_connected_at이 1분 이상 전
        boolean allDevicesOffline = devices.stream().allMatch(d -> {
            LocalDateTime last = d.getLastConnectedAt();
            return last == null
                || ChronoUnit.MINUTES.between(last, now) >= OFFLINE_THRESHOLD_MINUTES;
        });
        if (allDevicesOffline) {
            return "OFFLINE";
        }

        // 3. 외출: 모든 디바이스의 최근 1분 내 isPresent=true 인 vital이 없음
        // 즉, 디바이스별 최신 vital이 모두 isPresent=false 이고
        //     그 vital이 1분 이내에 기록된 경우 (1분 이상 미감지 상태가 지속)
        if (latestVitals.isEmpty()) {
            return "OFFLINE";
        }

        boolean anyPresent = latestVitals.stream()
            .anyMatch(v -> v.isPresent()
                && ChronoUnit.MINUTES.between(v.getRecordedAt(), now) < AWAY_THRESHOLD_MINUTES);

        if (!anyPresent) {
            return "AWAY";
        }

        // 4. 정상
        return "NORMAL";
    }
}
