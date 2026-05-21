package com.ikongserver.repository;

import com.ikongserver.entity.Device;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceRepository extends JpaRepository<Device, Long> {

    Optional<Device> findByUserId (Long userId);
    Optional<Device> findBySerialNum(String serialNum);

    // 한 피보호자의 모든 디바이스 조회 (멀티 센서 지원)
    List<Device> findAllByUserId(Long userId);

    // 마지막 연결 시각이 기준 시각 이전인 기기 조회 (연결 끊김 감지용)
    List<Device> findByLastConnectedAtBefore(LocalDateTime threshold);
}
