package com.ikongserver.service;

import static ch.qos.logback.core.spi.ComponentTracker.DEFAULT_TIMEOUT;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Service
public class SseService {

    private static final Long SSE_TIMEOUT = 60L * 1000 * 60;

    // 프론트엔드들의 파이프를 저장하는 스레드 안전한 Map
    // key: userId, value: SssEmitter
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    // 연결 유지 시간 (1시간)
    public SseEmitter subscribe(Long userId) {
        // 파이브 생성
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        // map 저장
        emitters.put(userId, emitter);
        log.info("SSE 연결 성공 - User ID: {}", userId);

        // 3. 파이프가 끊겼을 때(완료, 시간 초과, 에러) Map에서 찌꺼기 삭제
        emitter.onCompletion(() -> emitters.remove(userId));
        emitter.onTimeout(() -> emitters.remove(userId));
        emitter.onError((e) -> emitters.remove(userId));

        // 4. 503 에러 방지용 더미 데이터 전송
        // (처음 연결 시 아무 데이터도 안 보내면 브라우저가 에러로 인식할 수 있음)
        sendToClient(userId, "connect", "연결되었습니다. 데이터를 대기 중입니다.");

        return emitter;

    }

    // 라즈베리 파이에서 데이터가 들어왔을 때, 프론트엔드로 쏴주는 메서드
    public void sendVitalDataToClient(Long userId, Object data) {
        sendToClient(userId, "vital", data);
    }

    //실제 데이터를 파이프에 밀어 넣는 공통 메서드
    private void sendToClient(Long userId, String eventName, Object data) {
        SseEmitter emitter = emitters.get(userId);

        // 해당 유저가 현재 앱/웹을 켜두고 있어서 파이프가 존재한다면
        if (emitter != null) {
            try {
                // 프론트엔드로 이벤트 이름과 함께 데이터를 쏩니다!
                emitter.send(SseEmitter.event()
                    .name(eventName) // 이벤트 이름 (예: "vital")
                    .data(data));    // 실제 데이터 (JSON으로 자동 변환됨)
            } catch (IOException e) {
                // 전송 중 에러가 나면 (예: 사용자가 갑자기 브라우저를 끔) 파이프 삭제
                emitters.remove(userId);
                log.error("SSE 전송 오류 - User ID: {}", userId, e);
            }
        }
    }
}
