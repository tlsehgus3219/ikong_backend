package com.ikongserver.repository;

import com.ikongserver.entity.Device;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceRepository extends JpaRepository<Device, Long> {

    Optional<Device> findByUserId (Long userId);
    Optional<Device> findBySerialNum(String serialNum);

    // 한 피보호자의 모든 디바이스 조회 (멀티 센서 지원)
    List<Device> findAllByUserId(Long userId);
}
