package com.ikongserver.service;

import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class KakaoAuthService {

    private static final String KAKAO_USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";

    public String getKakaoSocialId(String kakaoAccessToken) {
        RestClient restClient = RestClient.create();

        Map<?, ?> response = restClient.get()
                .uri(KAKAO_USER_INFO_URL)
                .header("Authorization", "Bearer " + kakaoAccessToken)
                .retrieve()
                .body(Map.class);

        if (response == null || response.get("id") == null) {
            throw new RuntimeException("카카오 사용자 정보를 가져올 수 없습니다.");
        }

        return String.valueOf(response.get("id"));
    }
}
