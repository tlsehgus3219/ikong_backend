package com.ikongserver.dto;

import com.ikongserver.entity.ConditionType;
import java.util.List;

public class UserConditionDto {

    // 질환 목록 등록/수정 요청 — conditions가 빈 리스트이면 전체 삭제
    public record UpdateConditionsRequest(List<ConditionType> conditions) {

    }

    // 현재 등록된 질환 목록 응답
    public record ConditionsResponse(List<ConditionType> conditions) {

    }
}
