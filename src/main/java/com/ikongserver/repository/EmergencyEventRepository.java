package com.ikongserver.repository;

import com.ikongserver.entity.EmergencyEvent;
import com.ikongserver.entity.Users;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmergencyEventRepository extends JpaRepository<EmergencyEvent, Long> {

    boolean existsByUserAndStatus(Users user, String status);

    Optional<EmergencyEvent> findTopByUserAndStatusOrderByCreatedAtDesc(Users user, String status);

    long countByUserInAndStatus(List<Users> users, String status);

    List<EmergencyEvent> findByUserInOrderByCreatedAtDesc(List<Users> users);

    List<EmergencyEvent> findByUserInAndStatus(List<Users> users, String status);

    long countByUserAndStatus(Users user, String status);
}
