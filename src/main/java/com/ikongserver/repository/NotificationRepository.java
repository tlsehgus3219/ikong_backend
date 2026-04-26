package com.ikongserver.repository;

import com.ikongserver.entity.Notification;
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
}
