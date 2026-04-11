package com.ikongserver.controller;

import com.ikongserver.dto.DeviceDto.ResponseDevice;
import com.ikongserver.service.DeviceService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/devices")
public class DeviceController {

    private final DeviceService deviceService;

    // 특정 유저의 디바이스 연결 상태 목록 조회
    @GetMapping("/{userId}/devices")
    public ResponseEntity<List<ResponseDevice>> getDevices(@PathVariable Long userId) {

        List<ResponseDevice> deviceStatuses = deviceService.getDeviceStatus(userId);

        return ResponseEntity.ok(deviceStatuses);
    }

}
