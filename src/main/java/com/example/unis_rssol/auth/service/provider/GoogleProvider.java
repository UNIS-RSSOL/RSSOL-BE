package com.example.unis_rssol.auth.service.provider;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.MultiValueMap;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.json.JSONObject;

@Component
public class GoogleProvider implements SocialProvider {

    @Value("${oauth.google.client-id}")
    private String clientId;

    @Value("${oauth.google.client-secret}")
    private String clientSecret;

    @Value("${oauth.google.redirect-uri}")
    private String redirectUri;

    private final RestTemplate rt = new RestTemplate();

    @Override
    public boolean supports(String provider) {
        return "google".equalsIgnoreCase(provider);
    }

    @Override
    public String getAccessTokenFromCode(String code) {
        String url = "https://oauth2.googleapis.com/token";
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", code);
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("redirect_uri", redirectUri);
        params.add("grant_type", "authorization_code");

        ResponseEntity<String> res = rt.postForEntity(url, new HttpEntity<>(params, new HttpHeaders()), String.class);
        return new JSONObject(res.getBody()).getString("access_token");
    }

    @Override
    public SocialProfile fetchProfile(String accessToken) {
        String url = "https://www.googleapis.com/oauth2/v3/userinfo";
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(accessToken);
        ResponseEntity<String> res = rt.exchange(url, HttpMethod.GET, new HttpEntity<>(h), String.class);
        JSONObject o = new JSONObject(res.getBody());
        return new SocialProfile("google", o.getString("sub"), o.optString("name", null), null);
    }
}
