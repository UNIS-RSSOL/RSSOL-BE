package com.example.unis_rssol.auth.service.provider;

import org.json.JSONObject;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class KakaoProvider implements SocialProvider {
    private static final String TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String PROFILE_URL = "https://kapi.kakao.com/v2/user/me";

    private final RestTemplate rt = new RestTemplate();

    @Override
    public boolean supports(String provider) {
        return "kakao".equalsIgnoreCase(provider);
    }

    @Override
    public String getAccessTokenFromCode(String code) {
        // 👉 이 부분은 client_id, redirect_uri 등 application.yml 값 읽어와서 추가해야 함
        throw new UnsupportedOperationException("카카오는 별도 AccessToken 발급 로직 필요 (추가 구현)");
    }

    @Override
    public SocialProfile fetchProfile(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.set("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");

        ResponseEntity<String> response = rt.exchange(
                PROFILE_URL,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        JSONObject body = new JSONObject(response.getBody());

        String id = String.valueOf(body.getLong("id"));
        String nickname = null;
        String email = null;
        String profileImage = null;

        if (body.has("kakao_account")) {
            var account = body.getJSONObject("kakao_account");
            if (account.has("profile")) {
                var profile = account.getJSONObject("profile");
                nickname = profile.optString("nickname", null);
                profileImage = profile.optString("profile_image_url", null);
            }
            if (account.has("email")) {
                email = account.optString("email", null);
            }
        }

        return new SocialProfile(
                "kakao",
                id,
                nickname,
                email,
                profileImage
        );
    }
}
