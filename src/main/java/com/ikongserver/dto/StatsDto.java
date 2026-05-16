package com.ikongserver.dto;

import java.util.List;

public class StatsDto {

    // 그래프 및 하단 목록의 단일 데이터 포인트 (label: "N시" 또는 "MM/DD", value: bpm)
    public record GraphPoint(String label, int value) {}

    // 생체 통계 응답 — 평균/최소/최대 + 그래프 데이터(오름차순) + 하단 목록(내림차순)
    public record VitalStatsResponse(
        int avg,
        int min,
        int max,
        List<GraphPoint> graphData,   // 그래프 표시용 (오름차순)
        List<GraphPoint> detailList   // 하단 시간별 목록 (내림차순)
    ) {}
}
