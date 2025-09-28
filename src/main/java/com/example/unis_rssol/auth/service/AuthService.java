package com.example.unis_rssol.auth.service;

import com.example.unis_rssol.auth.dto.LoginResponse;
import com.example.unis_rssol.auth.dto.RefreshTokenResponse;
import com.example.unis_rssol.auth.entity.UserRefreshToken;
import com.example.unis_rssol.auth.repository.UserRefreshTokenRepository;
import com.example.unis_rssol.auth.service.provider.KakaoProvider;
import com.example.unis_rssol.auth.service.provider.SocialProfile;
import com.example.unis_rssol.global.config.JwtTokenProvider;
import com.example.unis_rssol.user.entity.AppUser;
import com.example.unis_rssol.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AppUserRepository users;
    private final UserRefreshTokenRepository refreshRepo;
    private final JwtTokenProvider jwt;
    private final KakaoProvider kakao;

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String KAKAO_LOGOUT_URL = "https://kapi.kakao.com/v1/user/logout";

    // 카카오 로그인 처리
    @Transactional
    public LoginResponse handleKakaoCallback(String code) {
        log.info("handleKakaoCallback called.");

        // 1) code → accessToken
        String accessToken = kakao.getAccessTokenFromCode(code);

        // 2) accessToken → profile
        SocialProfile profile = kakao.fetchProfile(accessToken);
        String kakaoId = profile.getProviderId();
        log.info("Kakao profile fetched: id={}, email={}", kakaoId, profile.getEmail());

        // 3) DB 저장 (신규는 생성, 기존은 accessToken 갱신)
        AppUser user = users.findByProviderAndProviderId("kakao", kakaoId).orElse(null);
        boolean isNewUser = (user == null);
        if (isNewUser) {
            user = users.save(AppUser.builder()
                    .provider("kakao")
                    .providerId(kakaoId)
                    .username(profile.getUsername())
                    .email(profile.getEmail())
                    .profileImageUrl(profile.getProfileImageUrl())
                    .kakaoAccessToken(accessToken) // 신규 저장
                    .build());
            log.info("New Kakao user saved. id={}", user.getId());
        } else {
            user.setKakaoAccessToken(accessToken); // 기존 사용자 갱신
            users.save(user);
        }

        // 4) JWT 발급 & Refresh 저장
        String at = jwt.generateAccess(user.getId());
        String rt = jwt.generateRefresh(user.getId());

        refreshRepo.deleteByUser(user);
        refreshRepo.save(UserRefreshToken.builder()
                .user(user)
                .refreshToken(rt)
                .expiresAt(LocalDateTime.now().plusDays(14))
                .build());

        // 5) 응답 ( *** activeStoreId 추가 !!!! )
        return new LoginResponse(
                at, rt, user.getId(), isNewUser,
                user.getUsername(), user.getEmail(), user.getProfileImageUrl(),
                user.getProvider(), user.getProviderId(),
                user.getActiveStoreId()
        );
    }

    // Refresh Token → Access Token 재발급
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

    // 로그아웃
    @Transactional
    public void logout(Long userId) {
        AppUser user = users.findById(userId).orElseThrow();

        // 카카오 로그아웃 호출 (카카오 사용자만)
        if ("kakao".equals(user.getProvider()) && user.getKakaoAccessToken() != null) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.add("Authorization", "Bearer " + user.getKakaoAccessToken());
                HttpEntity<Void> request = new HttpEntity<>(headers);

                restTemplate.postForObject(KAKAO_LOGOUT_URL, request, String.class);
                log.info("Kakao logout successful for userId={}", userId);
            } catch (Exception e) {
                log.warn("Kakao logout failed for userId={}", userId, e);
            }
        }

        // Refresh Token 삭제
        refreshRepo.deleteByUser(user);

        // DB에 저장된 카카오 AccessToken 무효화
        user.setKakaoAccessToken(null);
        users.save(user);
    }
}
