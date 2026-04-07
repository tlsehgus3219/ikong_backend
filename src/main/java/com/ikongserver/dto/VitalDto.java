package com.ikongserver.dto;

public class VitalDto {
    // 실시간 심박 및 호흡
    // int는 값이 없을 때 0 이고 Integer은 값이 없을 때 NULL을 넣을 수 있음
    public record ResponseUserVital(Long id, Integer heartRate, Integer breathRate) {

    }
}
