package com.ikongserver.service;

import com.ikongserver.dto.VitalDto.ResponseUserVital;
import com.ikongserver.dto.VitalDto.VitalRequestDto;
import com.ikongserver.entity.Device;
import com.ikongserver.entity.Users;
import com.ikongserver.entity.Vital;
import com.ikongserver.repository.DeviceRepository;
import com.ikongserver.repository.VitalRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VitalService {

    private final VitalRepository vitalRepository;
    private final EmergencyEventService emergencyEventService;
    private final DeviceRepository deviceRepository;
    private final SseService sseService;

    // 라즈베리파이에서 데이터 받아오기
    // 기기별로 마지막으로 Vital 데이터를 저장한 시간을 기록하는 메모리 맵
    // Key: 기기 시리얼 넘버, Value: 마지막 저장 시간(Timestamp)
    private final Map<String, Long> lastSavedTimeMap = new ConcurrentHashMap<>();

    @Transactional
    public void getVitalData(VitalRequestDto vitalDto) {
        String serialNum = vitalDto.serialNum();

        // 라즈베리와 연결이 되어 있는지 확인
        Device device = deviceRepository.findBySerialNum(serialNum)
            .orElseThrow(() -> new IllegalArgumentException("기기를 찾을 수 없습니다."));
        // 매초 마다 업데이트 되는 데이터를 device 테이블 안에서는 1분마다 연결 되어 있다는 마지막 연결 시간을 업데이트 함.
        device.updateLastConnectedAt();

        // 디바이스에서 피보호자 객체 찾기
        Users user = device.getUser();

        // 센서 종류 분기 — 시리얼 접두사 기준 (FALL-*: 낙상 전용 / HR-* 등: 심박·호흡 전용)
        if (serialNum != null && serialNum.startsWith("FALL")) {
            // 낙상 센서: 낙상 감지만 수행하고 HR/BR/SSE/저장은 건너뜀
            emergencyEventService.checkFallEvent(vitalDto, user, device);
            return;
        }

        // HR-* 센서 (또는 미지정 시리얼): 심박·호흡 처리만 수행, 낙상 검사는 생략
        // 데이터 안정화 (알고리즘 필터 통과)
        int stabilizedHR = filterNoise_HR(serialNum, vitalDto.heartRate());
        int stabilizedBR = filterNoise_BR(serialNum, vitalDto.breathRate());

        // 긴급 상황 검사 및 상태 반환
        boolean isHREvent = emergencyEventService.checkHeartBreathEvent(vitalDto, user, device);

        // 프론트엔드로 실시간 SSE 전송
        ResponseUserVital sseData = new ResponseUserVital(user.getId(), stabilizedHR, stabilizedBR,
            isHREvent);
        sseService.sendVitalDataToClient(user.getId(), sseData);

        long currentTime = System.currentTimeMillis();
        long lastSavedTime = lastSavedTimeMap.getOrDefault(serialNum, 0L);

        // 받은 데이터를 Vital 테이블에 저장
        if (currentTime - lastSavedTime >= 10000) {
            Vital newVital = Vital.builder()
                .user(user)
                .device(device)
                .heartRate(stabilizedHR)
                .breathRate(stabilizedBR)
                .isFallDetected(false) // HR 센서는 낙상 정보 없음
                .isPresent(vitalDto.isPresent())
                .build();
            vitalRepository.save(newVital);
            lastSavedTimeMap.put(serialNum, currentTime);
        }
    }

    // 💡 1차 검사관 (Median): 문서 명세에 따라 윈도우 사이즈 3 (약 6초 분량 데이터)
    private static final int WINDOW_SIZE = 3;
    private final Map<String, LinkedList<Integer>> hrWindowMap = new ConcurrentHashMap<>();
    private final Map<String, LinkedList<Integer>> brWindowMap = new ConcurrentHashMap<>();

    // 💡 2차 검사관 (EMA): 문서 권장값 (0.3 ~ 0.4 사이의 최적값 0.35 적용)
    // 너무 낮으면(0.1) 10~20초 지연이 발생하므로 0.35가 가장 적절합니다.
    private static final double EMA_ALPHA = 0.35;
    private final Map<String, Integer> lastEmaHrMap = new ConcurrentHashMap<>();
    private final Map<String, Integer> lastEmaBrMap = new ConcurrentHashMap<>();

    // --- 🛠️ 명세서 기반 하이브리드 안정화 알고리즘 (Median + EMA) ---

    private int filterNoise_HR(String serialNum, int rawHeartRate) {
        return applyHybridFilter(serialNum, rawHeartRate, hrWindowMap, lastEmaHrMap);
    }

    private int filterNoise_BR(String serialNum, int rawBreathRate) {
        return applyHybridFilter(serialNum, rawBreathRate, brWindowMap, lastEmaBrMap);
    }

    // 공통 필터 로직
    private int applyHybridFilter(String serialNum, int rawValue,
        Map<String, LinkedList<Integer>> windowMap,
        Map<String, Integer> emaMap) {

        // 1. 기기별 윈도우 바구니 가져오기
        LinkedList<Integer> window = windowMap.computeIfAbsent(serialNum, k -> new LinkedList<>());

        // 2. 새 데이터 추가 및 오래된 데이터 제거 (최대 3개 유지)
        window.add(rawValue);
        if (window.size() > WINDOW_SIZE) {
            window.removeFirst();
        }

        // 3. 데이터가 3개 미만일 때는 초기화 과정이므로 원본 반환
        if (window.size() < WINDOW_SIZE) {
            emaMap.put(serialNum, rawValue);
            return rawValue;
        }

        // 4. [Median Filter] 3개 데이터 중 중간값 추출 (Pi가 놓친 미세 스파이크 최종 방어)
        List<Integer> sortedWindow = new ArrayList<>(window);
        Collections.sort(sortedWindow);
        int medianValue = sortedWindow.get(WINDOW_SIZE / 2);

        // 5. [EMA Filter] 이전 평균값과 현재 중간값을 가중치(0.35)로 결합 (유일한 스무딩 단계)
        int lastEma = emaMap.getOrDefault(serialNum, medianValue);
        int finalSmoothedValue = (int) Math.round((medianValue * EMA_ALPHA) + (lastEma * (1.0 - EMA_ALPHA)));

        // 6. 계산된 최종 값을 EMA 장부에 기록
        emaMap.put(serialNum, finalSmoothedValue);

        return finalSmoothedValue;
    }

}
