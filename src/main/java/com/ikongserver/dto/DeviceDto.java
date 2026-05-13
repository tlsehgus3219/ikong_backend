package com.ikongserver.dto;

public class DeviceDto {

    // 단일 디바이스 연결 정보 (기존 호환)
    public record ResponseDevice(Long id, boolean isConnected, String serialNum) {}

    // 피보호자의 모든 센서 연결 상태
    public record DeviceStatusResponse(
        boolean raspberryConnected,
        boolean heartSensorConnected,
        boolean fallSensorConnected
    ) {}
}
