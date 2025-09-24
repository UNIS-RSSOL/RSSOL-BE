package com.example.unis_rssol.auth.service.provider;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.MultiValueMap;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.json.JSONObject;

@Component
public class NaverProvider implements SocialProvider {

    @Value("${oauth.naver.client-id}")
    private String clientId;

    @Value("${oauth.naver.client-secret}")
    private String clientSecret;

    @Value("${oauth.naver.redirect-uri}")
    private String redirectUri;

    private final RestTemplate rt = new RestTemplate();

    @Override
    public boolean supports(String provider) {
        return "naver".equalsIgnoreCase(provider);
    }

    @Override
    public String getAccessTokenFromCode(String code) {
        String url = "https://nid.naver.com/oauth2.0/token";
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
        String url = "https://openapi.naver.com/v1/nid/me";
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(accessToken);
        ResponseEntity<String> res = rt.exchange(url, HttpMethod.GET, new HttpEntity<>(h), String.class);
        JSONObject resp = new JSONObject(res.getBody()).getJSONObject("response");
        return new SocialProfile("naver", resp.getString("id"), resp.optString("name", null), resp.optString("mobile", null));
    }
}
