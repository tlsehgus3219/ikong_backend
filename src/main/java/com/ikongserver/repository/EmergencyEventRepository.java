package com.ikongserver.repository;

import com.ikongserver.entity.EmergencyEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmergencyEventRepository extends JpaRepository<EmergencyEvent, Long> {
}
