package com.ikongserver.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

@Slf4j
@Configuration
public class FirebaseConfig {

    @PostConstruct
    public void initialize() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                InputStream serviceAccount =
                    new ClassPathResource("firebase-service-account.json").getInputStream();
                FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();
                FirebaseApp.initializeApp(options);
                log.info("Firebase Admin SDK 초기화 성공");
            }
        } catch (IOException e) {
            log.warn("firebase-service-account.json 파일을 찾을 수 없습니다. FCM 기능이 비활성화됩니다.");
        }
    }
}
