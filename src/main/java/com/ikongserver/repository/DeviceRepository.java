package com.ikongserver.repository;

import com.ikongserver.entity.Device;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceRepository extends JpaRepository<Device, Long> {

    List<Device> findAllByUserId (Long userId);
    Optional<Device> findBySerialNum(String serialNum);
}
