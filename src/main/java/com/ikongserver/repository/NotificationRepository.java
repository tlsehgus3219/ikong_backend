package com.ikongserver.repository;

import com.ikongserver.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findByGuardianId(Long guardianId, Pageable pageable);
    Page<Notification> findByGuardianIdAndStatus(Long guardianId, String status, Pageable pageable);
}
