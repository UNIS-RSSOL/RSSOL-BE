package com.example.unis_rssol.auth.service;

import com.example.unis_rssol.auth.dto.LoginResponse;
import com.example.unis_rssol.auth.dto.RefreshTokenResponse;
import com.example.unis_rssol.auth.entity.UserRefreshToken;
import com.example.unis_rssol.auth.repository.UserRefreshTokenRepository;
import com.example.unis_rssol.auth.service.provider.SocialProfile;
import com.example.unis_rssol.auth.service.provider.SocialProvider;
import com.example.unis_rssol.global.config.JwtTokenProvider;
import com.example.unis_rssol.user.entity.AppUser;
import com.example.unis_rssol.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;   // ✅ 로거 추가
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j   // ✅ 로거 활성화
public class AuthService {

    private final AppUserRepository users;
    private final UserRefreshTokenRepository refreshRepo;
    private final JwtTokenProvider jwt;
    private final List<SocialProvider> providers;

    /**
     * OAuth2 Callback 처리 (Google, Kakao, Naver)
     */
    @Transactional
    public LoginResponse handleCallback(String providerKey, String code) {
        log.info("🔑 handleCallback called. provider={}, code={}", providerKey, code);

        // 1. provider 선택
        SocialProvider provider = providers.stream()
                .filter(sp -> sp.supports(providerKey))
                .findFirst()
                .orElseThrow(() -> {
                    log.error("❌ Unsupported provider: {}", providerKey);
                    return new IllegalArgumentException("Unsupported provider");
                });

        // 2. code → accessToken 교환
        log.info("➡️ Exchanging code for access token...");
        String accessToken = provider.getAccessTokenFromCode(code);
        log.info("✅ Access token received: {}", accessToken);

        // 3. accessToken → 사용자 프로필 조회
        log.info("➡️ Fetching profile from provider={}...", providerKey);
        SocialProfile profile = provider.fetchProfile(accessToken);
        log.info("✅ Profile fetched: providerId={}, username={}, phone={}",
                profile.getProviderId(), profile.getUsername(), profile.getPhoneNumber());

        // 4. DB 조회 (기존 유저 여부 확인)
        AppUser user = users.findByProviderAndProviderId(profile.getProvider(), profile.getProviderId())
                .orElse(null);

        boolean isNewUser = (user == null);
        if (isNewUser) {
            log.info("🆕 New user detected. Saving to DB...");
            user = users.save(AppUser.builder()
                    .provider(profile.getProvider())
                    .providerId(profile.getProviderId())
                    .username(profile.getUsername())
                    .phoneNumber(profile.getPhoneNumber())
                    .build());
            log.info("✅ User saved with id={}", user.getId());
        } else {
            log.info("👤 Existing user found with id={}", user.getId());
        }

        // 5. JWT 발급
        String at = jwt.generateAccess(user.getId());
        String rt = jwt.generateRefresh(user.getId());
        log.info("🎟️ JWT tokens generated. accessToken={}, refreshToken={}", at, rt);

        // 6. 기존 refresh 토큰 제거 후 새로 저장
        refreshRepo.deleteByUser(user);
        refreshRepo.save(UserRefreshToken.builder()
                .user(user)
                .refreshToken(rt)
                .expiresAt(LocalDateTime.now().plusDays(14))
                .build());
        log.info("🔄 Refresh token saved for user id={}", user.getId());

        // 7. 응답 반환
        log.info("✅ handleCallback success. Returning LoginResponse for user id={}", user.getId());
        return new LoginResponse(
                at,
                rt,
                user.getId(),
                isNewUser,
                user.getUsername(),
                user.getPhoneNumber(),
                user.getProvider(),
                user.getProviderId()
        );
    }

    /**
     * Refresh Token으로 Access Token 갱신
     */
    @Transactional(readOnly = true)
    public RefreshTokenResponse refresh(String refreshToken) {
        log.info("🔑 Refresh token request received: {}", refreshToken);

        var token = refreshRepo.findByRefreshToken(refreshToken)
                .orElseThrow(() -> {
                    log.error("❌ Invalid refresh token: {}", refreshToken);
                    return new IllegalArgumentException("Invalid refresh token");
                });

        if (token.getRevokedAt() != null || token.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.error("❌ Refresh token expired or revoked. userId={}, expiresAt={}",
                    token.getUser().getId(), token.getExpiresAt());
            throw new IllegalArgumentException("Expired or revoked refresh token");
        }

        String newAccess = jwt.generateAccess(token.getUser().getId());
        log.info("✅ New access token generated for user id={}", token.getUser().getId());
        return new RefreshTokenResponse(newAccess);
    }
}
