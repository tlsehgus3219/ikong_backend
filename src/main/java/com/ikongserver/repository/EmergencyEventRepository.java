package com.ikongserver.repository;

import com.ikongserver.dto.EventDto;
import com.ikongserver.entity.EmergencyEvent;
import com.ikongserver.entity.Users;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmergencyEventRepository extends JpaRepository<EmergencyEvent, Long> {

    boolean existsByUserAndStatus(Users user,String status);
    Optional<EmergencyEvent> findTopByUserAndStatusOrderByCreatedAtDesc(Users user, String status);
}