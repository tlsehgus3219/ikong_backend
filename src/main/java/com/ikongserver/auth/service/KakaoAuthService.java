package com.ikongserver.auth.service;

import java.time.LocalDate;
import java.util.Map;
import lombok.Getter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class KakaoAuthService {

    private static final String KAKAO_USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";

    @Getter
    public static class KakaoUserInfo {
        private final String socialId;
        private final String name;
        private final String phone;
        private final LocalDate birthDate;

        public KakaoUserInfo(String socialId, String name, String phone, LocalDate birthDate) {
            this.socialId = socialId;
            this.name = name;
            this.phone = phone;
            this.birthDate = birthDate;
        }
    }

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

        Map<?, ?> account = (Map<?, ?>) response.get("kakao_account");

        String name = null;
        String phone = null;
        LocalDate birthDate = null;

        if (account != null) {
            // 이름
            if (account.get("name") != null) {
                name = String.valueOf(account.get("name"));
            }

            // 전화번호: "+82 10-1234-5678" → "010-1234-5678"
            if (account.get("phone_number") != null) {
                phone = convertPhone(String.valueOf(account.get("phone_number")));
            }

            // 생년: "1990" → LocalDate 1990-01-01
            if (account.get("birthyear") != null) {
                try {
                    int year = Integer.parseInt(String.valueOf(account.get("birthyear")));
                    birthDate = LocalDate.of(year, 1, 1);
                } catch (NumberFormatException ignored) {}
            }
        }

        return new KakaoUserInfo(socialId, name, phone, birthDate);
    }

    // 기존 메서드 유지 (하위 호환)
    public String getKakaoSocialId(String kakaoAccessToken) {
        return getKakaoUserInfo(kakaoAccessToken).getSocialId();
    }

    private String convertPhone(String kakaoPhone) {
        // "+82 10-1234-5678" or "+82 1012345678" → "010-1234-5678"
        String digits = kakaoPhone.replaceAll("[^0-9]", "");
        if (digits.startsWith("82")) {
            digits = "0" + digits.substring(2);
        }
        if (digits.length() == 11) {
            return digits.substring(0, 3) + "-" + digits.substring(3, 7) + "-" + digits.substring(7);
        }
        return digits;
    }
}
