package com.ikongserver.dto;

import java.time.LocalDateTime;

public class EventDto {

    // 낙상 감지
    public record ResponseEvent(Long id, String eventType, String status,
                                    LocalDateTime createdAt) {

    }

    // 피보호자가 직접 119 또는 보호자 한테 연락 버튼을 눌러 연락함
    public record RequestCallEvent(Long userId, String callType, Long targetGuardianId,
                                   Long eventId) {

    }
}
