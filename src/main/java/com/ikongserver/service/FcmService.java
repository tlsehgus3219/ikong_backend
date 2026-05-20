package com.ikongserver.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class FcmService {

    // FCM 푸시 알림 전송 — notificationType으로 앱에서 일반/긴급 알림 화면 구분
    // notificationType: "ALERT"(주의) or "EMERGENCY"(심각/낙상)
    public void sendPushNotification(String fcmToken, String title, String body, String notificationType) {
        if (fcmToken == null || fcmToken.isBlank()) {
            return;
        }
        if (FirebaseApp.getApps().isEmpty()) {
            log.warn("Firebase 미초기화 상태 — FCM 발송 건너뜀: {}", title);
            return;
        }
        try {
            Message message = Message.builder()
                .setToken(fcmToken)
                .setNotification(Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build())
                .putData("notificationType", notificationType)
                .build();
            String response = FirebaseMessaging.getInstance().send(message);
            log.info("FCM 발송 성공 [{}]: {}", notificationType, response);
        } catch (FirebaseMessagingException e) {
            log.error("FCM 발송 실패: {}", e.getMessage());
        }
    }
}
