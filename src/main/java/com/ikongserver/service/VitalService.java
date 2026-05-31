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

    private final Map<String, Long> lastSavedTimeMap = new ConcurrentHashMap<>();

    @Transactional
    public void getVitalData(VitalRequestDto vitalDto) {
        String serialNum = vitalDto.serialNum();

        Device device = deviceRepository.findBySerialNum(serialNum)
            .orElseThrow(() -> new IllegalArgumentException("기기를 찾을 수 없습니다."));
        device.updateLastConnectedAt();

        Users user = device.getUser();

        // 낙상 센서: 낙상 감지만 수행하고 HR/BR/SSE/저장은 건너뜀
        if (serialNum != null && serialNum.startsWith("FALL")) {
            emergencyEventService.checkFallEvent(vitalDto, user, device);
            return;
        }

        // HR 센서: 필터링(range check → Median(5) → EMA(0.3)) 후 심박·호흡 처리
        int hr = filter(serialNum, vitalDto.heartRate(), HR_MIN, HR_MAX, hrWindowMap, lastEmaHrMap);
        int br = filter(serialNum, vitalDto.breathRate(), BR_MIN, BR_MAX, brWindowMap, lastEmaBrMap);

        boolean isHREvent = emergencyEventService.checkHeartBreathEvent(hr, br, user, device);

        sseService.sendVitalDataToClient(user.getId(), new ResponseUserVital(user.getId(), hr, br, isHREvent));

        long currentTime = System.currentTimeMillis();
        long lastSavedTime = lastSavedTimeMap.getOrDefault(serialNum, 0L);

        if (currentTime - lastSavedTime >= 10000) {
            vitalRepository.save(Vital.builder()
                .user(user)
                .device(device)
                .heartRate(hr)
                .breathRate(br)
                .isFallDetected(false)
                .isPresent(vitalDto.isPresent())
                .build());
            lastSavedTimeMap.put(serialNum, currentTime);
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

        if (raw < min || raw > max) {
            return emaMap.getOrDefault(serialNum, raw);
        }

        LinkedList<Integer> window = windowMap.computeIfAbsent(serialNum, k -> new LinkedList<>());
        window.add(raw);
        if (window.size() > WINDOW_SIZE) {
            window.removeFirst();
        }

        List<Integer> sorted = new ArrayList<>(window);
        Collections.sort(sorted);
        int median = sorted.get(sorted.size() / 2);

        int lastEma = emaMap.getOrDefault(serialNum, median);
        int result = (int) Math.round(median * EMA_ALPHA + lastEma * (1.0 - EMA_ALPHA));
        emaMap.put(serialNum, result);

        return result;
    }
}
