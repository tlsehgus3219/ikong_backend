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
        // 라즈베리와 연결이 되어 있는지 확인
        Device device = deviceRepository.findBySerialNum(vitalDto.serialNum())
            .orElseThrow(() -> new IllegalArgumentException("기기를 찾을 수 없습니다."));
        // 매초 마다 업데이트 되는 데이터를 device 테이블 안에서는 1분마다 연결 되어 있다는 마지막 연결 시간을 업데이트 함.
        device.updateLastConnectedAt();

        // 디바이스에서 피보호자 객체 찾기
        Users user = device.getUser();

        // 데이터 안정화 (알고리즘 필터 통과)
        int stabilizedHR = filterNoise_HR(vitalDto.serialNum(), vitalDto.heartRate());
        int stabilizedBR = filterNoise_BR(vitalDto.serialNum(), vitalDto.breathRate());

        // 긴급 상황 검사 및 상태 반환
        boolean status = false;
        boolean isFallEvent = emergencyEventService.checkFallEvent(vitalDto, user, device);
        boolean isHREvent = emergencyEventService.checkHeartBreathEvent(vitalDto, user, device);

        if (isFallEvent || isHREvent) {
            status = true;
        }

        // 프론트엔드로 실시간 SSE 전송
        ResponseUserVital sseData = new ResponseUserVital(user.getId(), stabilizedHR, stabilizedBR,
            status);
        sseService.sendVitalDataToClient(user.getId(), sseData);

        long currentTime = System.currentTimeMillis();
        long lastSavedTime = lastSavedTimeMap.getOrDefault(vitalDto.serialNum(), 0L);

        // 받은 데이터를 Vital 테이블에 저장
        if (currentTime - lastSavedTime >= 10000) {
            Vital newVital = Vital.builder()
                .user(user)
                .device(device)
                .heartRate(stabilizedHR)
                .breathRate(stabilizedBR)
                .isFallDetected(status)
                .isPresent(vitalDto.isPresent())
                .build();
            vitalRepository.save(newVital);
            lastSavedTimeMap.put(vitalDto.serialNum(), currentTime);
        }
    }

    // 1차 필터 (Median): 윈도우 사이즈 5 (약 5초 분량 데이터)
    private static final int WINDOW_SIZE = 5;
    private final Map<String, LinkedList<Integer>> hrWindowMap = new ConcurrentHashMap<>();
    private final Map<String, LinkedList<Integer>> brWindowMap = new ConcurrentHashMap<>();

    // 2차 필터 (EMA): alpha 0.2 — 급격한 변화 억제, 실제 추세는 반영
    private static final double EMA_ALPHA = 0.2;
    private final Map<String, Integer> lastEmaHrMap = new ConcurrentHashMap<>();
    private final Map<String, Integer> lastEmaBrMap = new ConcurrentHashMap<>();

    // 생리학적으로 불가능한 값은 윈도우에 넣지 않고 이전 안정화 값 유지
    private static final int HR_MIN = 30, HR_MAX = 200;
    private static final int BR_MIN = 8, BR_MAX = 40;

    // 직전 EMA 대비 초당 20bpm 이상 급변하면 스파이크로 간주
    private static final int HR_MAX_DELTA = 20;

    private int filterNoise_HR(String serialNum, int rawHeartRate) {
        if (rawHeartRate < HR_MIN || rawHeartRate > HR_MAX) {
            return lastEmaHrMap.getOrDefault(serialNum, rawHeartRate);
        }
        int currentEma = lastEmaHrMap.getOrDefault(serialNum, rawHeartRate);
        if (Math.abs(rawHeartRate - currentEma) > HR_MAX_DELTA) {
            return currentEma;
        }
        return applyHybridFilter(serialNum, rawHeartRate, hrWindowMap, lastEmaHrMap);
    }

    private int filterNoise_BR(String serialNum, int rawBreathRate) {
        if (rawBreathRate < BR_MIN || rawBreathRate > BR_MAX) {
            return lastEmaBrMap.getOrDefault(serialNum, rawBreathRate);
        }
        return applyHybridFilter(serialNum, rawBreathRate, brWindowMap, lastEmaBrMap);
    }

    // 공통 필터 로직
    private int applyHybridFilter(String serialNum, int rawValue,
        Map<String, LinkedList<Integer>> windowMap,
        Map<String, Integer> emaMap) {

        // 1. 기기별 윈도우 바구니 가져오기
        LinkedList<Integer> window = windowMap.computeIfAbsent(serialNum, k -> new LinkedList<>());

        window.add(rawValue);
        if (window.size() > WINDOW_SIZE) {
            window.removeFirst();
        }

        if (window.size() < WINDOW_SIZE) {
            emaMap.put(serialNum, rawValue);
            return rawValue;
        }

        List<Integer> sortedWindow = new ArrayList<>(window);
        Collections.sort(sortedWindow);
        int medianValue = sortedWindow.get(WINDOW_SIZE / 2);

        int lastEma = emaMap.getOrDefault(serialNum, medianValue);
        int finalSmoothedValue = (int) Math.round((medianValue * EMA_ALPHA) + (lastEma * (1.0 - EMA_ALPHA)));

        emaMap.put(serialNum, finalSmoothedValue);

        return finalSmoothedValue;
    }

}
