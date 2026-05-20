package com.ikongserver.repository;

import com.ikongserver.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // 보호자 기준 전체 조회
    Page<Notification> findByGuardianId(Long guardianId, Pageable pageable);

    // 보호자 기준 이벤트 상태(PENDING/RESOLVED) 필터 조회
    @Query("SELECT n FROM Notification n WHERE n.guardian.id = :guardianId AND n.emergencyEvent.status = :eventStatus")
    Page<Notification> findByGuardianIdAndEventStatus(@Param("guardianId") Long guardianId, @Param("eventStatus") String eventStatus, Pageable pageable);

    // 피보호자(userId) 기준 전체 조회
    @Query("SELECT n FROM Notification n WHERE n.emergencyEvent.user.id = :userId")
    Page<Notification> findByUserId(@Param("userId") Long userId, Pageable pageable);

    // 피보호자(userId) 기준 이벤트 상태 필터 조회
    @Query("SELECT n FROM Notification n WHERE n.emergencyEvent.user.id = :userId AND n.emergencyEvent.status = :eventStatus")
    Page<Notification> findByUserIdAndEventStatus(@Param("userId") Long userId, @Param("eventStatus") String eventStatus, Pageable pageable);

    long countByGuardianIdAndReadYN(Long guardianId, String readYN);
}
