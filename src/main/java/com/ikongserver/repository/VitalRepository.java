package com.ikongserver.repository;

import com.ikongserver.entity.Vital;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VitalRepository extends JpaRepository<Vital, Long> {

    /**
     * 한 피보호자의 모든 디바이스에 대해 가장 최근 vital 1건씩 조회.
     * PostgreSQL DISTINCT ON 사용으로 단일 쿼리로 처리.
     */
    @Query(value = """
        SELECT DISTINCT ON (v.device_id) v.*
        FROM vital v
        WHERE v.user_id = :userId
        ORDER BY v.device_id, v.recorded_at DESC
        """, nativeQuery = true)
    List<Vital> findLatestVitalPerDevice(@Param("userId") Long userId);
}
