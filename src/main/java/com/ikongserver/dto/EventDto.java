package com.ikongserver.dto;

import java.time.LocalDateTime;

public class EventDto {

    // 낙상 감지
    public record ResponseFallEvent(Long id, String eventType, String status,
                                    LocalDateTime detectedAt) {

    }

    public record RequestCallEvent(Long userId, String callType, Long targetGuardianId,
                                   Long eventId) {

    }
}
