package com.example.unis_rssol.auth.service;

import com.example.unis_rssol.auth.dto.LoginResponse;
import com.example.unis_rssol.auth.dto.RefreshTokenResponse;
import com.example.unis_rssol.auth.entity.UserRefreshToken;
import com.example.unis_rssol.auth.repository.UserRefreshTokenRepository;
import com.example.unis_rssol.global.config.JwtTokenProvider;
import com.example.unis_rssol.user.entity.AppUser;
import com.example.unis_rssol.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.json.JSONObject;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AppUserRepository users;
    private final UserRefreshTokenRepository refreshRepo;
    private final JwtTokenProvider jwt;

    private final String kakaoClientId = "YOUR_KAKAO_CLIENT_ID";
    private final String kakaoClientSecret = "YOUR_KAKAO_CLIENT_SECRET";
    private final String kakaoRedirectUri = "http://localhost:8080/api/auth/kakao/callback";

    /**
     * 카카오 로그인 처리
     */
    @Transactional
    public LoginResponse handleKakaoCallback(String code) {
        log.info("🔑 handleKakaoCallback called. code={}", code);

        // 1. code → accessToken 교환
        String accessToken = getAccessTokenFromKakao(code);
        log.info("✅ AccessToken={}", accessToken);

        // 2. accessToken → 사용자 프로필 조회
        JSONObject profile = fetchKakaoProfile(accessToken);
        String kakaoId = profile.getString("id");
        JSONObject kakaoAccount = profile.getJSONObject("kakao_account");

        String nickname = kakaoAccount.getJSONObject("profile").optString("nickname", null);
        String profileImage = kakaoAccount.getJSONObject("profile").optString("profile_image_url", null);
        String email = kakaoAccount.optString("email", null);

        log.info("✅ Kakao profile fetched: id={}, nickname={}, email={}", kakaoId, nickname, email);

        // 3. DB 저장 (신규/기존 확인)
        AppUser user = users.findByProviderAndProviderId("kakao", kakaoId).orElse(null);
        boolean isNewUser = (user == null);

        if (isNewUser) {
            user = users.save(AppUser.builder()
                    .provider("kakao")
                    .providerId(kakaoId)
                    .username(nickname)
                    .email(email)
                    .profileImageUrl(profileImage)
                    .build());
            log.info("✅ New Kakao user saved. id={}", user.getId());
        }

        // 4. JWT 발급
        String at = jwt.generateAccess(user.getId());
        String rt = jwt.generateRefresh(user.getId());

        refreshRepo.deleteByUser(user);
        refreshRepo.save(UserRefreshToken.builder()
                .user(user)
                .refreshToken(rt)
                .expiresAt(LocalDateTime.now().plusDays(14))
                .build());

        log.info("🎟️ JWT tokens issued for Kakao user id={}", user.getId());

        return new LoginResponse(
                at,
                rt,
                user.getId(),
                isNewUser,
                user.getUsername(),
                user.getEmail(),
                user.getProfileImageUrl(),
                user.getProvider(),
                user.getProviderId()
        );
    }

    /**
     * 카카오 토큰 교환
     */
    private String getAccessTokenFromKakao(String code) {
        String url = "https://kauth.kakao.com/oauth/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", kakaoClientId);
        params.add("client_secret", kakaoClientSecret);
        params.add("redirect_uri", kakaoRedirectUri);
        params.add("code", code);

        RestTemplate rt = new RestTemplate();
        ResponseEntity<String> response = rt.postForEntity(url, new HttpEntity<>(params, headers), String.class);
        JSONObject body = new JSONObject(response.getBody());

        return body.getString("access_token");
    }

    /**
     * 카카오 사용자 정보 요청
     */
    private JSONObject fetchKakaoProfile(String accessToken) {
        String url = "https://kapi.kakao.com/v2/user/me";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.set("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");

        RestTemplate rt = new RestTemplate();
        ResponseEntity<String> response = rt.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
        return new JSONObject(response.getBody());
    }

    /**
     * Refresh Token → Access Token 재발급
     */
    @Transactional(readOnly = true)
    public RefreshTokenResponse refresh(String refreshToken) {
        var token = refreshRepo.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        if (token.getRevokedAt() != null || token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Expired or revoked refresh token");
        }

        String newAccess = jwt.generateAccess(token.getUser().getId());
        return new RefreshTokenResponse(newAccess);
    }
}
