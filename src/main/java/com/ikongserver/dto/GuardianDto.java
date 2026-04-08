package com.ikongserver.dto;

public class GuardianDto {

    // 등록된 보호자 (피보호자 화면)
    public record ResponseGuardian(Long id, String name, String phone, boolean isPrimary,
                                   String relation) {
    }

    // 보호자 등록 요청
    public record RequestRegister(String name, String phone, String relation, boolean isPrimary) {
    }

    // 보호자 등록 응답
    public record ResponseRegister(Long guardianId, String name, String phone, String relation,
                                   boolean isPrimary, boolean isActive) {
    }
}
