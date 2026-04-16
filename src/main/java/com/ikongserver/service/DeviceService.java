package com.ikongserver.service;

import com.ikongserver.dto.DeviceDto;
import com.ikongserver.dto.DeviceDto.ResponseDevice;
import com.ikongserver.entity.Device;
import com.ikongserver.repository.DeviceRepository;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeviceService {

    private final DeviceRepository deviceRepository;

    // 연결 된 디바이스(라즈베리파이) 조회
    public ResponseDevice getDeviceStatus(Long userId) {

        Device device = deviceRepository.findByUserId(userId)
            .orElseThrow(() -> new IllegalArgumentException("연결 된 디바이스가 없습니다."));

        boolean isConnected = checkConnection(device.getLastConnectedAt());

        return new DeviceDto.ResponseDevice(
            device.getId(),
            isConnected,
            device.getSerialNum()
        );
    }

    // 디바이스(라즈베리파이) 연결 조회
    private boolean checkConnection(LocalDateTime lastConnectedAt) {
        if (lastConnectedAt == null) {
            return false; // 한 번도 데이터가 들어온 적이 없으면 연결 안 됨
        }
        // 마지막 연결 시간과 현재 시간의 차이를 계산
        long minAgo = ChronoUnit.MINUTES.between(lastConnectedAt, LocalDateTime.now());
        return minAgo < 5; // 5분 이내에 데이터가 들어왔으면 true(연결됨)
    }

    // TODO: 디바이스 다시 연결 시도
    // 디바이스 연결 시도는 db에 기록이 남아야함 - DB 수정 필요 (연결 시간)
    //라즈베리 파이 기기 쪽에서 코드를 짤 때 "데이터 전송 실패 시 10초 뒤 재시도" 로직만 넣어주면 알아서 재연결 처리가 됩니다.


}