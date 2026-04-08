package com.ikongserver.repository;

import com.ikongserver.entity.Vital;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VitalRepository extends JpaRepository<Vital, Long> {

    Vital findTopByDeviceOrderByRecordedAtDesc(Long deviceId);

}
