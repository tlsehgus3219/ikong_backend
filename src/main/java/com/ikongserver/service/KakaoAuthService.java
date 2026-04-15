package com.ikongserver.service;

import java.time.LocalDate;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class KakaoAuthService {

    private static final String KAKAO_USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";

    public KakaoUserInfo getKakaoUserInfo(String kakaoAccessToken) {
        RestClient restClient = RestClient.create();

        Map<?, ?> response = restClient.get()
                .uri(KAKAO_USER_INFO_URL)
                .header("Authorization", "Bearer " + kakaoAccessToken)
                .retrieve()
                .body(Map.class);

        if (response == null || response.get("id") == null) {
            throw new RuntimeException("카카오 사용자 정보를 가져올 수 없습니다.");
        }

        String socialId = String.valueOf(response.get("id"));

        Map<?, ?> kakaoAccount = (Map<?, ?>) response.get("kakao_account");

        String name = null;
        String phone = null;
        LocalDate birthDate = null;

        if (kakaoAccount != null) {
            // 이름
            Map<?, ?> profile = (Map<?, ?>) kakaoAccount.get("profile");
            if (profile != null) {
                name = (String) profile.get("nickname");
            }

            // 전화번호 (+82 10-xxxx-xxxx → 010-xxxx-xxxx)
            String rawPhone = (String) kakaoAccount.get("phone_number");
            if (rawPhone != null) {
                phone = rawPhone.replace("+82 ", "0").replace("+82", "0");
            }

            // 생년 (birthyear만 수신 → YYYY-01-01)
            String birthyear = (String) kakaoAccount.get("birthyear");
            if (birthyear != null) {
                birthDate = LocalDate.of(Integer.parseInt(birthyear), 1, 1);
            }
        }

        return new KakaoUserInfo(socialId, name, phone, birthDate);
    }

    public record KakaoUserInfo(String socialId, String name, String phone, LocalDate birthDate) {}
}
