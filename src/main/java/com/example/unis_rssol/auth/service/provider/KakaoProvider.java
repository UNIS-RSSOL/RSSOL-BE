package com.example.unis_rssol.auth.service.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class KakaoProvider {

    @Value("${oauth.kakao.client-id}")
    private String clientId;

    @Value("${oauth.kakao.client-secret:}")
    private String clientSecret;

    @Value("${oauth.kakao.redirect-uri}")
    private String redirectUri;

    private static final String TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String PROFILE_URL = "https://kapi.kakao.com/v2/user/me";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 인가코드(code) -> Kakao AccessToken 교환
     */
    public String getAccessTokenFromCode(String code) {
        log.info("🔑 [KakaoProvider] Authorization Code 수신: {}", code);
        log.info("   [Check] client_id={}, redirect_uri={}, client_secret={}",
                clientId, redirectUri, (clientSecret == null || clientSecret.isBlank()) ? "(없음)" : "(설정됨)");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId);
        params.add("redirect_uri", redirectUri);
        params.add("code", code);
        if (clientSecret != null && !clientSecret.isBlank()) {
            params.add("client_secret", clientSecret);
        }

        log.info("📤 [KakaoProvider] 카카오로 전송할 파라미터={}", params);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(TOKEN_URL, request, String.class);
            log.debug("📩 [KakaoProvider] 토큰 응답 바디: {}", response.getBody());

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode accessTokenNode = root.get("access_token");
            if (accessTokenNode == null) {
                log.error("❌ [KakaoProvider] access_token 없음: {}", response.getBody());
                throw new IllegalArgumentException("카카오 access_token 파싱 실패");
            }
            String accessToken = accessTokenNode.asText();
            log.info("✅ [KakaoProvider] access_token 발급 성공");
            return accessToken;

        } catch (HttpClientErrorException e) {
            log.error("❌ [KakaoProvider] 토큰 요청 4xx 에러: status={}, body={}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new IllegalArgumentException("카카오 토큰 요청 실패", e);
        } catch (Exception e) {
            log.error("❌ [KakaoProvider] 토큰 요청 실패", e);
            throw new IllegalArgumentException("카카오 토큰 요청 실패", e);
        }
    }

    /**
     * Kakao AccessToken -> 사용자 프로필
     */
    public SocialProfile fetchProfile(String accessToken) {
        log.info("🔍 [KakaoProvider] 프로필 요청 시작 (Bearer 토큰 사용)");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    PROFILE_URL, HttpMethod.GET, request, String.class);
            log.debug("📩 [KakaoProvider] 프로필 응답 바디: {}", response.getBody());

            JsonNode root = objectMapper.readTree(response.getBody());

            String id = root.path("id").asText(); // 필수
            String nickname = root.path("properties").path("nickname").asText(null);
            if (nickname == null || nickname.isBlank()) {
                nickname = root.path("kakao_account").path("profile").path("nickname").asText("");
            }
            String profileImage = root.path("properties").path("profile_image").asText(null);
            if (profileImage == null || profileImage.isBlank()) {
                profileImage = root.path("kakao_account").path("profile").path("profile_image_url").asText("");
            }
            String email = root.path("kakao_account").path("email").asText("");

            SocialProfile profile = SocialProfile.builder()
                    .provider("kakao")
                    .providerId(id)
                    .username(nickname == null ? "" : nickname)
                    .email(email == null ? "" : email)
                    .profileImageUrl(profileImage == null ? "" : profileImage)
                    .build();

            log.info("✅ [KakaoProvider] 프로필 파싱 성공: id={}, email={}, nickname={}", id, email, nickname);
            return profile;

        } catch (HttpClientErrorException e) {
            log.error("❌ [KakaoProvider] 프로필 요청 4xx 에러: status={}, body={}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new IllegalArgumentException("카카오 프로필 요청 실패", e);
        } catch (Exception e) {
            log.error("❌ [KakaoProvider] 프로필 요청 실패", e);
            throw new IllegalArgumentException("카카오 프로필 요청 실패", e);
        }
    }
}
