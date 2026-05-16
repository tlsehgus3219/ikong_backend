package com.ikongserver.service;

import com.ikongserver.dto.StatsDto.GraphPoint;
import com.ikongserver.dto.StatsDto.VitalStatsResponse;
import com.ikongserver.dto.UserDto.MainProfileResponse;
import com.ikongserver.dto.UserDto.UserStateDetailResponse;
import com.ikongserver.entity.UserGuardianMap;
import com.ikongserver.entity.Users;
import com.ikongserver.entity.Vital;
import com.ikongserver.repository.EmergencyEventRepository;
import com.ikongserver.repository.UserGuardianMapRepository;
import com.ikongserver.repository.UsersRepository;
import com.ikongserver.repository.VitalRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

    @Transactional
    public void updateFcmToken(Long userId, String fcmToken) {
        Users user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        user.updateFcmToken(fcmToken);
    }

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

    // 생체 통계 조회 — type(HEART/BREATH) × period(TODAY/WEEK/MONTH) 조합으로 평균/최소/최대 및 그래프 데이터 반환
    public VitalStatsResponse getVitalStats(Long userId, Long guardianId, String type, String period) {

        // 보호자-피보호자 관계 검증
        userGuardianMapRepository.findByUserIdAndGuardianId(userId, guardianId)
            .orElseThrow(() -> new RuntimeException("권한이 없는 피보호자 입니다."));

        // type과 period 조합으로 적절한 쿼리 선택
        List<Object[]> rows = switch (type) {
            case "HEART" -> switch (period) {
                case "TODAY" -> vitalRepository.findHourlyHeartRateToday(userId);
                case "WEEK"  -> vitalRepository.findDailyHeartRateThisWeek(userId);
                case "MONTH" -> vitalRepository.findDailyHeartRateThisMonth(userId);
                default -> throw new IllegalArgumentException("잘못된 period 값입니다: " + period);
            };
            case "BREATH" -> switch (period) {
                case "TODAY" -> vitalRepository.findHourlyBreathRateToday(userId);
                case "WEEK"  -> vitalRepository.findDailyBreathRateThisWeek(userId);
                case "MONTH" -> vitalRepository.findDailyBreathRateThisMonth(userId);
                default -> throw new IllegalArgumentException("잘못된 period 값입니다: " + period);
            };
            default -> throw new IllegalArgumentException("잘못된 type 값입니다: " + type);
        };

        // 데이터 없으면 빈 응답 반환
        if (rows.isEmpty()) {
            return new VitalStatsResponse(0, 0, 0, List.of(), List.of());
        }

        // 전체 평균/최소/최대 계산
        int totalAvg = (int) rows.stream().mapToInt(r -> ((Number) r[1]).intValue()).average().orElse(0);
        int totalMin = rows.stream().mapToInt(r -> ((Number) r[2]).intValue()).min().orElse(0);
        int totalMax = rows.stream().mapToInt(r -> ((Number) r[3]).intValue()).max().orElse(0);

        // 그래프 데이터 — 오름차순 (TODAY: "N시", WEEK/MONTH: "MM/DD" 또는 "D일")
        List<GraphPoint> graphData = rows.stream()
            .map(r -> new GraphPoint(formatLabel(r[0], period), ((Number) r[1]).intValue()))
            .toList();

        // 하단 목록 — 내림차순 (최신 시간이 위로)
        List<GraphPoint> detailList = new ArrayList<>(graphData);
        Collections.reverse(detailList);

        return new VitalStatsResponse(totalAvg, totalMin, totalMax, graphData, detailList);
    }

    // TODAY: "N시" 포맷, WEEK/MONTH: 쿼리에서 이미 포맷된 문자열 그대로 사용
    private String formatLabel(Object raw, String period) {
        if ("TODAY".equals(period)) {
            return ((Number) raw).intValue() + "시";
        }
        return raw.toString();
    }
}
