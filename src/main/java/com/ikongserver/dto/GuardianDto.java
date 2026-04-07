package com.ikongserver.dto;

public class GuardianDto {

    // 등록된 보호자 (피보호자 화면)
    public record ResponseGuardian(Long id, String name, String phone, boolean isPrimary,
                                   String relation) {


    }

}
