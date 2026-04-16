package com.ikongserver.controller;

import com.ikongserver.dto.VitalDto.VitalRequestDto;
import com.ikongserver.service.SseService;
import com.ikongserver.service.VitalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/vitals")
public class VitalController {

    private final VitalService vitalService;
    private final SseService sseService; // 프론트엔드로 데이터를 쏴줄 파이프 관리자

    // 라즈베리파이에서 서버로 데이터 넣기
    @PostMapping
    public ResponseEntity<String> receiveVitalData(@RequestBody VitalRequestDto vitalDto){

        vitalService.getVitalData(vitalDto);
        return ResponseEntity.ok("데이터 수신 및 처리 완료");

    }

    // sse 연결
    @GetMapping(value = "/stream/{userId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeToVitals(@PathVariable Long userId) {

        // 프론트엔드와 연결된 '파이프(SseEmitter)'를 생성하고 반환합니다.
        // 연결이 끊기지 않고 계속 유지되면서 데이터가 스트리밍됩니다.
        return sseService.subscribe(userId);
    }

}
