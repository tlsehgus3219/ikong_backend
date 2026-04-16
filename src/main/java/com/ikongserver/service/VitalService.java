package com.ikongserver.service;

import com.ikongserver.dto.VitalDto.ResponseUserVital;
import com.ikongserver.dto.VitalDto.VitalRequestDto;
import com.ikongserver.entity.Device;
import com.ikongserver.entity.Users;
import com.ikongserver.entity.Vital;
import com.ikongserver.repository.DeviceRepository;
import com.ikongserver.repository.VitalRepository;
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
    // 라즈베리 파이 예상 데이터 구조
    /*
    payload = {
        "serialNum": "RPI-TEST-01",
        "heartRate": 85,
        "breathRate": 18,
        "isFallDetected": False,  # 파이썬은 대문자 False/True 입니다
        "isPresent": True
    }
    */

    @Transactional
    public void getVitalData(VitalRequestDto vitalDto) {

        // 라즈베리와 연결이 되어 있는지 확인
        Device device = deviceRepository.findBySerialNum(vitalDto.serialNum())
            .orElseThrow(() -> new IllegalArgumentException("기기를 찾을 수 없습니다."));

        // 매초 마다 업데이트 되는 데이터를 device 테이블 안에서는 1분마다 연결 되어 있다는 마지막 연결 시간을 업데이트 함.
        device.updateLastConnectedAt();

        Users user = device.getUser();

        // 받은 데이터를 Vital 테이블에 저장
        Vital newVital = Vital.builder()
            .user(user)
            .device(device)
            .heartRate(vitalDto.heartRate())
            .breathRate(vitalDto.breathRate())
            .isPresent(vitalDto.isPresent())
            .build();
        vitalRepository.save(newVital);

        // 라즈베리파이에서 받은 데이터 낙상검사 및 심박수, 호흡수 검사
        // 이벤트 발생시 EmergencyEvent 서비스에서 db에 저장
        emergencyEventService.checkFallEvent(vitalDto, user, device);
        emergencyEventService.checkHeartBreathEvent(vitalDto, user, device);

        // 찾은 주인의 아이디(Long)로 SSE 알림
        sseService.sendVitalDataToClient(user.getId(), vitalDto);
    }
}
