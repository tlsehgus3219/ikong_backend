package com.ikongserver.repository;

import com.ikongserver.entity.Vital;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VitalRepository extends JpaRepository<Vital, Long> {

    // 피보호자의 모든 디바이스에 대해 가장 최근 vital 1건씩 조회 (PostgreSQL DISTINCT ON 사용)
    @Query(value = """
        SELECT DISTINCT ON (v.device_id) v.*
        FROM vital v
        WHERE v.user_id = :userId
        ORDER BY v.device_id, v.recorded_at DESC
        """, nativeQuery = true)
    List<Vital> findLatestVitalPerDevice(@Param("userId") Long userId);

    // 피보호자의 가장 최근 vital 1건 조회
    Optional<Vital> findFirstByUserIdOrderByRecordedAtDesc(Long userId);

    // [오늘] 시간별 심박수 평균/최소/최대 — 결과: [hour, avg, min, max]
    @Query(value = """
        SELECT EXTRACT(HOUR FROM recorded_at)::int AS hour,
               ROUND(AVG(heart_rate))::int          AS avg_val,
               MIN(heart_rate)                       AS min_val,
               MAX(heart_rate)                       AS max_val
        FROM vital
        WHERE user_id = :userId
          AND recorded_at >= CURRENT_DATE
          AND recorded_at < CURRENT_DATE + INTERVAL '1 day'
        GROUP BY EXTRACT(HOUR FROM recorded_at)
        ORDER BY hour
        """, nativeQuery = true)
    List<Object[]> findHourlyHeartRateToday(@Param("userId") Long userId);

    // [오늘] 시간별 호흡수 평균/최소/최대 — 결과: [hour, avg, min, max]
    @Query(value = """
        SELECT EXTRACT(HOUR FROM recorded_at)::int AS hour,
               ROUND(AVG(breath_rate))::int         AS avg_val,
               MIN(breath_rate)                      AS min_val,
               MAX(breath_rate)                      AS max_val
        FROM vital
        WHERE user_id = :userId
          AND recorded_at >= CURRENT_DATE
          AND recorded_at < CURRENT_DATE + INTERVAL '1 day'
        GROUP BY EXTRACT(HOUR FROM recorded_at)
        ORDER BY hour
        """, nativeQuery = true)
    List<Object[]> findHourlyBreathRateToday(@Param("userId") Long userId);

    // [이번 주] 일별 심박수 평균/최소/최대 — 결과: [label(MM/DD), avg, min, max]
    @Query(value = """
        SELECT TO_CHAR(recorded_at, 'MM/DD') AS label,
               ROUND(AVG(heart_rate))::int   AS avg_val,
               MIN(heart_rate)               AS min_val,
               MAX(heart_rate)               AS max_val
        FROM vital
        WHERE user_id = :userId
          AND recorded_at >= DATE_TRUNC('week', CURRENT_DATE)
        GROUP BY DATE(recorded_at), TO_CHAR(recorded_at, 'MM/DD')
        ORDER BY DATE(recorded_at)
        """, nativeQuery = true)
    List<Object[]> findDailyHeartRateThisWeek(@Param("userId") Long userId);

    // [이번 주] 일별 호흡수 평균/최소/최대 — 결과: [label(MM/DD), avg, min, max]
    @Query(value = """
        SELECT TO_CHAR(recorded_at, 'MM/DD') AS label,
               ROUND(AVG(breath_rate))::int  AS avg_val,
               MIN(breath_rate)              AS min_val,
               MAX(breath_rate)              AS max_val
        FROM vital
        WHERE user_id = :userId
          AND recorded_at >= DATE_TRUNC('week', CURRENT_DATE)
        GROUP BY DATE(recorded_at), TO_CHAR(recorded_at, 'MM/DD')
        ORDER BY DATE(recorded_at)
        """, nativeQuery = true)
    List<Object[]> findDailyBreathRateThisWeek(@Param("userId") Long userId);

    // [이번 달] 일별 심박수 평균/최소/최대 — 결과: [label(D일), avg, min, max]
    @Query(value = """
        SELECT TO_CHAR(recorded_at, 'DD일') AS label,
               ROUND(AVG(heart_rate))::int  AS avg_val,
               MIN(heart_rate)              AS min_val,
               MAX(heart_rate)              AS max_val
        FROM vital
        WHERE user_id = :userId
          AND recorded_at >= DATE_TRUNC('month', CURRENT_DATE)
        GROUP BY DATE(recorded_at), TO_CHAR(recorded_at, 'DD일')
        ORDER BY DATE(recorded_at)
        """, nativeQuery = true)
    List<Object[]> findDailyHeartRateThisMonth(@Param("userId") Long userId);

    // [이번 달] 일별 호흡수 평균/최소/최대 — 결과: [label(D일), avg, min, max]
    @Query(value = """
        SELECT TO_CHAR(recorded_at, 'DD일') AS label,
               ROUND(AVG(breath_rate))::int AS avg_val,
               MIN(breath_rate)             AS min_val,
               MAX(breath_rate)             AS max_val
        FROM vital
        WHERE user_id = :userId
          AND recorded_at >= DATE_TRUNC('month', CURRENT_DATE)
        GROUP BY DATE(recorded_at), TO_CHAR(recorded_at, 'DD일')
        ORDER BY DATE(recorded_at)
        """, nativeQuery = true)
    List<Object[]> findDailyBreathRateThisMonth(@Param("userId") Long userId);
}
