package com.ikongserver.service;

import com.ikongserver.dto.VitalDto;
import com.ikongserver.entity.Device;
import com.ikongserver.entity.Users;
import com.ikongserver.entity.Vital;
import com.ikongserver.repository.DeviceRepository;
import com.ikongserver.repository.UserRepository;
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
    @Transactional
    public void getVitalData(VitalDto.VitalRequestDto vitalDto) {

        Device device = deviceRepository.findBySerialNum(vitalDto.serialNum())
            .orElseThrow(() -> new IllegalArgumentException("기기를 찾을 수 없습니다."));

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

        // 라즈베리파이에서 받은 데이터 낙상검사 및 심박수. 호흡수 검사
        // 이벤트 발생시 EmergencyEvent 서비스에서 db에 저장

        emergencyEventService.checkFallEvent(vitalDto, user, device);
        emergencyEventService.checkHeartBreathEvent(vitalDto, user, device);

        // 4. 찾은 주인의 아이디(Long)로 SSE 알림을 쏩니다!
        // 이렇게 하면 타입 에러도 없고, 완벽하게 해당 유저의 프론트엔드로 데이터가 날아갑니다.
        sseService.sendVitalDataToClient(user.getId(), vitalDto);

    }
}
