package com.ikongserver.dto;

import java.time.LocalDateTime;
import java.util.List;

public class EventDto {

    // 낙상 감지
    public record ResponseEvent(Long id, String eventType, String status,
                                LocalDateTime createdAt) {

    }

    // 피보호자가 직접 119 또는 보호자 한테 연락 버튼을 눌러 연락함
    public record RequestCallEvent(Long userId, String callType, Long targetGuardianId,
                                   Long eventId) {

    }

    // 라즈베리파이 LCD [알림] 버튼 → 보호자 즉시 알림 요청
    public record ManualAlertRequest(String serialNum) {

    }

    // 보호자 화면 해결건/미해결건 요약
    public record EventSummaryResponse(long resolvedCount, long unresolvedCount) {

    }

    // 보호자 화면 긴급 알림 목록
    public record EmergencyAlertResponse(
        Long id,
        String userName,
        String eventType,
        String status,
        LocalDateTime createdAt,
        String detail
    ) {

    }

    public record EmergencyAlertListResponse(List<EmergencyAlertResponse> alerts) {

    }
}
