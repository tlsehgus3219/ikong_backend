package com.ikongserver.service;

import com.ikongserver.dto.VitalDto.ResponseUserVital;
import com.ikongserver.dto.VitalDto.VitalRequestDto;
import com.ikongserver.entity.Device;
import com.ikongserver.entity.Users;
import com.ikongserver.entity.Vital;
import com.ikongserver.repository.DeviceRepository;
import com.ikongserver.repository.VitalRepository;
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
        int stabilizedHR = filterNoise_HR(vitalDto.heartRate());
        int stabilizedBR = filterNoise_BR(vitalDto.breathRate());

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

    // --- 데이터 안정화 알고리즘 더미 (추후 로직 채워넣을 예정) ---
    private int filterNoise_HR(int rawHeartRate) {
        return rawHeartRate; // 일단 들어온 값 그대로 반환
    }

    private int filterNoise_BR(int rawBreathRate) {
        return rawBreathRate; // 일단 들어온 값 그대로 반환
    }

}
