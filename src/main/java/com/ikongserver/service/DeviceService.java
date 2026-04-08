package com.ikongserver.service;

import com.ikongserver.dto.DeviceDto;
import com.ikongserver.entity.Device;
import com.ikongserver.entity.Vital;
import com.ikongserver.repository.DeviceRepository;
import com.ikongserver.repository.VitalRepository;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final VitalRepository vitalRepository;

    // 디바이스 조회
    // 디바이스는 2개
    public List<DeviceDto.ResponseDevice> getDeviceStatus(Long userId) {

        List<Device> devices = deviceRepository.findAllByUserId(userId);

        if (devices.isEmpty()) {
            return List.of();
        }

        return devices.stream().map(device -> {
            boolean isConnected = checkConnection(device.getId());

            return new DeviceDto.ResponseDevice(
                device.getId(),
                device.getDeviceType(),
                isConnected,
                device.getSerialNum()
            );
        }).toList();

    }

    // 디바이스 연결 조회
    private boolean checkConnection(Long deviceId) {
        Vital lastVital = vitalRepository.findTopByDeviceOrderByRecordedAtDesc(deviceId);

        if (lastVital == null) {
            return false;
        }

        long minAgo = ChronoUnit.MINUTES.between(lastVital.getRecordedAt(), LocalDateTime.now());
        return minAgo < 5;
    }

    // TODO: 디바이스 다시 연결 시도
    // 디바이스 연결 시도는 db에 기록이 남아야함


}