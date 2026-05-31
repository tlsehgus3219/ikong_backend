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

        // 필터링 (range check → Median(5) → EMA(0.3))
        int hr = filter(vitalDto.serialNum(), vitalDto.heartRate(), HR_MIN, HR_MAX, hrWindowMap, lastEmaHrMap);
        int br = filter(vitalDto.serialNum(), vitalDto.breathRate(), BR_MIN, BR_MAX, brWindowMap, lastEmaBrMap);

        // 긴급 상황 검사 — 30초 연속 이상이 노이즈 필터 역할
        boolean isFallEvent = emergencyEventService.checkFallEvent(vitalDto, user, device);
        boolean isHREvent = emergencyEventService.checkHeartBreathEvent(hr, br, user, device);
        boolean status = isFallEvent || isHREvent;

        // 프론트엔드로 실시간 SSE 전송
        sseService.sendVitalDataToClient(user.getId(), new ResponseUserVital(user.getId(), hr, br, status));

        long currentTime = System.currentTimeMillis();
        long lastSavedTime = lastSavedTimeMap.getOrDefault(vitalDto.serialNum(), 0L);

        // 10초마다 DB 저장
        if (currentTime - lastSavedTime >= 10000) {
            vitalRepository.save(Vital.builder()
                .user(user)
                .device(device)
                .heartRate(hr)
                .breathRate(br)
                .isFallDetected(status)
                .isPresent(vitalDto.isPresent())
                .build());
            lastSavedTimeMap.put(vitalDto.serialNum(), currentTime);
        }
    }

    private static final int HR_MIN = 30, HR_MAX = 200;
    private static final int BR_MIN = 8, BR_MAX = 40;
    private static final int WINDOW_SIZE = 5;
    private static final double EMA_ALPHA = 0.3;

    private final Map<String, LinkedList<Integer>> hrWindowMap = new ConcurrentHashMap<>();
    private final Map<String, LinkedList<Integer>> brWindowMap = new ConcurrentHashMap<>();
    private final Map<String, Integer> lastEmaHrMap = new ConcurrentHashMap<>();
    private final Map<String, Integer> lastEmaBrMap = new ConcurrentHashMap<>();

    private int filter(String serialNum, int raw, int min, int max,
        Map<String, LinkedList<Integer>> windowMap, Map<String, Integer> emaMap) {

        // 생리적 범위 벗어난 값은 직전 EMA 유지
        if (raw < min || raw > max) {
            return emaMap.getOrDefault(serialNum, raw);
        }

        // Median(5)
        LinkedList<Integer> window = windowMap.computeIfAbsent(serialNum, k -> new LinkedList<>());
        window.add(raw);
        if (window.size() > WINDOW_SIZE) {
            window.removeFirst();
        }

        List<Integer> sorted = new ArrayList<>(window);
        Collections.sort(sorted);
        int median = sorted.get(sorted.size() / 2);

        // EMA(0.3)
        int lastEma = emaMap.getOrDefault(serialNum, median);
        int result = (int) Math.round(median * EMA_ALPHA + lastEma * (1.0 - EMA_ALPHA));
        emaMap.put(serialNum, result);

        return result;
    }

}
