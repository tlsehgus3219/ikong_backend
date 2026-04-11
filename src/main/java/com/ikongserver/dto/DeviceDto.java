package com.ikongserver.dto;

public class DeviceDto {

    // 디바이스 연결 유무
    public record ResponseDevice(Long id, boolean isConnected,
                                 String serialNum) {

    }
}
