package com.ikongserver.dto;

import java.time.LocalDateTime;
import java.util.List;

public class GuardianMainDto {

    /**
     * 상단 카운트 (긴급 / 주의 / 외출 / 전체)
     */
    public record DashboardSummary(
        long emergencyCount,
        long warningCount,
        long awayCount,
        long totalCount
    ) {

    }

    /**
     * 최근 응급 이벤트 정보
     */
    public record LatestEvent(
        Long eventId,
        String eventType,    // "FALL" | "HEART_ISSUE" | "BREATH_ISSUE"
        String status,       // "PENDING" | "RESOLVED"
        LocalDateTime createdAt
    ) {

    }

    /**
     * 피보호자 카드 (보호자 메인 화면)
     * status: "EMERGENCY" | "AWAY" | "OFFLINE" | "NORMAL"
     */
    public record UserCard(
        Long userId,
        String name,
        String phone,
        String relation,
        boolean isPrimary,
        String status,
        Integer heartRate,
        Integer breathRate,
        LatestEvent latestEvent
    ) {

    }

    /**
     * 피보호자 목록 응답
     */
    public record UserListResponse(
        long total,
        List<UserCard> users
    ) {

    }
}
