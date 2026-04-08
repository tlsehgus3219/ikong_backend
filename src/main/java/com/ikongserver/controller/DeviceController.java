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
@RequestMapping("/api/device")
public class DeviceController {

    private final DeviceService deviceService;

    // 디바이스 연결 상태
    @GetMapping("{userId}")
    public ResponseEntity<List<ResponseDevice>> getDevice(@PathVariable Long userId) {

        List<ResponseDevice> deviceStatus = deviceService.getDeviceStatus(userId);

        return ResponseEntity.ok(deviceStatus);
    }

}
