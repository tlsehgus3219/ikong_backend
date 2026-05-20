package com.ikongserver.repository;

import com.ikongserver.entity.EmergencyEvent;
import com.ikongserver.entity.Notification;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // 보호자 기준 조회
    Page<Notification> findByGuardianId(Long guardianId, Pageable pageable);
    Page<Notification> findByGuardianIdAndStatus(Long guardianId, String status, Pageable pageable);

    // 피보호자(userId) 기준 조회
    @Query("SELECT n FROM Notification n WHERE n.emergencyEvent.user.id = :userId")
    Page<Notification> findByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT n FROM Notification n WHERE n.emergencyEvent.user.id = :userId AND n.status = :status")
    Page<Notification> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") String status, Pageable pageable);

    // 재알림 스케줄러용: 같은 이벤트의 가장 최근 알림 (마지막 발송 시각 확인용)
    Optional<Notification> findTopByEmergencyEventOrderBySentAtDesc(EmergencyEvent event);

    // 재알림 시 메시지 재사용을 위한 첫 알림 조회
    Optional<Notification> findTopByEmergencyEventOrderBySentAtAsc(EmergencyEvent event);
}
