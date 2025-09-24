package com.example.unis_rssol.auth.service.provider;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.MultiValueMap;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.json.JSONObject;

@Component
public class KakaoProvider implements SocialProvider {

    @Value("${oauth.kakao.client-id}")
    private String clientId;

    @Value("${oauth.kakao.client-secret}")
    private String clientSecret;

    @Value("${oauth.kakao.redirect-uri}")
    private String redirectUri;

    private final RestTemplate rt = new RestTemplate();

    @Override
    public boolean supports(String provider) {
        return "kakao".equalsIgnoreCase(provider);
    }

    @Override
    public String getAccessTokenFromCode(String code) {
        String url = "https://kauth.kakao.com/oauth/token";
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("redirect_uri", redirectUri);
        params.add("code", code);

        ResponseEntity<String> res = rt.postForEntity(url, new HttpEntity<>(params, new HttpHeaders()), String.class);
        return new JSONObject(res.getBody()).getString("access_token");
    }

    @Override
    public SocialProfile fetchProfile(String accessToken) {
        String url = "https://kapi.kakao.com/v2/user/me";
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(accessToken);
        ResponseEntity<String> res = rt.exchange(url, HttpMethod.GET, new HttpEntity<>(h), String.class);
        JSONObject body = new JSONObject(res.getBody());
        String id = String.valueOf(body.getLong("id"));
        String username = body.getJSONObject("kakao_account")
                .getJSONObject("profile")
                .optString("nickname", null);
        String phone = body.getJSONObject("kakao_account").optString("phone_number", null);
        return new SocialProfile("kakao", id, username, phone != null ? phone.replace("+82 ", "0") : null);
    }
}
