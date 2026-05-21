package com.ikongserver.service;

import com.ikongserver.entity.Device;
import com.ikongserver.entity.Guardian;
import com.ikongserver.entity.UserGuardianMap;
import com.ikongserver.repository.DeviceRepository;
import com.ikongserver.repository.UserGuardianMapRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceMonitorService {

    // 기기 연결 끊김 기준 — 10분 이상 데이터 없으면 연결 끊김으로 판단
    private static final long DISCONNECT_THRESHOLD_MINUTES = 10;

    // 알림 쿨다운 — 연결 끊김 알림 30분마다 재발송 (반복 알림 방지)
    private static final long ALERT_COOLDOWN_MS = 30 * 60 * 1000L;
    private final Map<Long, Long> lastAlertTimeMap = new ConcurrentHashMap<>();

    private final DeviceRepository deviceRepository;
    private final UserGuardianMapRepository userGuardianMapRepository;
    private final FcmService fcmService;

    // 1분마다 기기 연결 상태 확인
    @Transactional
    @Scheduled(fixedDelay = 60000)
    public void checkDisconnectedDevices() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(DISCONNECT_THRESHOLD_MINUTES);
        List<Device> disconnectedDevices = deviceRepository.findByLastConnectedAtBefore(threshold);

        for (Device device : disconnectedDevices) {
            Long deviceId = device.getId();
            long now = System.currentTimeMillis();
            Long lastAlertTime = lastAlertTimeMap.get(deviceId);

            if (lastAlertTime != null && now - lastAlertTime < ALERT_COOLDOWN_MS) {
                continue; // 쿨다운 중
            }

            lastAlertTimeMap.put(deviceId, now);

            List<UserGuardianMap> mappings = userGuardianMapRepository.findByUser(device.getUser())
                .stream().filter(m -> "Y".equals(m.getIsActive())).toList();

            for (UserGuardianMap mapping : mappings) {
                Guardian guardian = mapping.getGuardian();
                fcmService.sendPushNotification(
                    guardian.getFcmToken(),
                    "기기 연결 끊김",
                    device.getUser().getName() + "님의 기기와 연결이 끊겼습니다. 확인해주세요.",
                    "ALERT"
                );
            }
            log.info("기기 연결 끊김 알림 발송 — deviceId: {}", deviceId);
        }
    }
}
