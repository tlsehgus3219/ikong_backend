package com.ikongserver.repository;

import com.ikongserver.entity.EmergencyEvent;
import com.ikongserver.entity.Users;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmergencyEventRepository extends JpaRepository<EmergencyEvent, Long> {

    boolean existsByUserAndStatus(Users user, String status);

    Optional<EmergencyEvent> findTopByUserAndStatusOrderByCreatedAtDesc(Users user, String status);

    // 한 피보호자의 특정 상태 이벤트 개수 (예: PENDING 이벤트 건수)
    long countByUserAndStatus(Users user, String status);

    // 중복 이벤트 방지: 같은 user + eventType의 PENDING 이벤트가 이미 있는지 확인
    boolean existsByUserAndEventTypeAndStatus(Users user, String eventType, String status);

    // 재알림 스케줄러용: 특정 상태의 모든 이벤트 (예: PENDING 전부)
    List<EmergencyEvent> findAllByStatus(String status);
}
