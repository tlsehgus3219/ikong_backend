package com.ikongserver.dto;

public class UserDto {

    // 피보호자 (이름, 수치(정상, 비정상, 이상))
    public record MainProfileResponse(Long id, String name, String status) {

    }


}
