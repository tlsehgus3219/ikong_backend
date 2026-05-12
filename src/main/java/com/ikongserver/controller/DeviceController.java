package com.ikongserver.controller;

import com.ikongserver.dto.DeviceDto;
import com.ikongserver.service.DeviceService;
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

    @GetMapping("/{userId}/devices")
    public ResponseEntity<DeviceDto.DeviceStatusResponse> getDevices(@PathVariable Long userId) {
        return ResponseEntity.ok(deviceService.getDeviceStatus(userId));
    }

}
