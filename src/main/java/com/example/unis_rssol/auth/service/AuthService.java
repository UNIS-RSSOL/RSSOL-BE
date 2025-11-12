package com.example.unis_rssol.auth.service;

import com.example.unis_rssol.auth.dto.LoginResponse;
import com.example.unis_rssol.auth.dto.RefreshTokenResponse;
import com.example.unis_rssol.auth.entity.UserRefreshToken;
import com.example.unis_rssol.auth.repository.UserRefreshTokenRepository;
import com.example.unis_rssol.auth.service.provider.KakaoProvider;
import com.example.unis_rssol.auth.service.provider.SocialProfile;
import com.example.unis_rssol.global.config.JwtTokenProvider;
import com.example.unis_rssol.global.exception.UnauthorizedException;
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

        // ⭐️ [수정 1] 방어 코드: 프로필 조회 실패 또는 ID가 없는 경우
        if (profile == null || profile.getProviderId() == null) {
            log.error("Failed to fetch Kakao profile or ID is null. accessToken: {}", accessToken);
            // 이 경우, 적절한 예외(e.g., CAuthenticationEntryPoint)를 발생시켜
            throw new UnauthorizedException("카카오 프로필 정보를 가져오는데 실패했습니다.");
        }

        // ⭐️ [수정 2] SocialProfile DTO에 isDefaultImage() 메서드가 구현되어 있다고 가정합니다.
        // (예: private boolean isDefaultImage; ... getter)
        // 카카오 응답의 is_default_image 값을 DTO가 받아줘야 합니다.

        // ⭐️ [수정 3] DB에 저장할 최종 프로필 URL 및 사용자 이름 결정
        String kakaoProfileUrl = profile.getProfileImageUrl();
        String finalProfileImageUrl; // ⭐️ 기본값 설정 제거

// ⭐️ [수정 3-1] URL 자체에 기본 프로필 문자열이 포함되어 있는지 확인
        boolean isKakaoDefault = (kakaoProfileUrl == null) ||
                (kakaoProfileUrl.contains("default_profile.jpeg"));

        if (isKakaoDefault) {
            // 기본 이미지거나 null이면, 우리 staff.png를 저장
            finalProfileImageUrl = "https://rssol-bucket.s3.ap-northeast-2.amazonaws.com/staff.png";
        } else {
            // 기본 이미지가 아니면 (즉, 사용자 커스텀 이미지이면) 카카오 URL을 그대로 저장
            finalProfileImageUrl = kakaoProfileUrl;
        }

        // 닉네임이 null일 경우 (미동의 등) 기본값 설정
        String finalUsername = profile.getUsername();
        if (finalUsername == null) {
            finalUsername = "사용자"; // 또는 서비스 기본 닉네임
            log.info("Kakao username is null. Setting to default '사용자'.");
        }

        // 3) DB 저장 (신규는 생성, 기존은 accessToken 갱신)
        AppUser user = users.findByProviderAndProviderId("kakao", kakaoId).orElse(null);
        boolean isNewUser = (user == null);
        if (isNewUser) {
            user = users.save(AppUser.builder()
                    .provider("kakao")
                    .providerId(kakaoId)
                    .username(finalUsername) // ⭐️ [수정] profile.getUsername() -> finalUsername
                    .email(profile.getEmail())
                    .profileImageUrl(finalProfileImageUrl) // null 또는 실제 URL
                    .kakaoAccessToken(accessToken) // 신규 저장
                    .build());
            log.info("New Kakao user saved. id={}", user.getId());
        } else {
            user.setKakaoAccessToken(accessToken); // 기존 사용자 갱신
            // ⭐️ [수정 5] 기존 사용자: 토큰과 함께 프로필 정보도 갱신
            // (카카오에서 닉네임이나 프로필 사진을 변경했을 수 있으므로)
            user.setUsername(finalUsername);
            user.setEmail(profile.getEmail()); // 이메일도 갱신
            user.setProfileImageUrl(finalProfileImageUrl); // 프로필 이미지 갱신

            // @Transactional 환경이므로 user 객체는 dirty-checking 되어
            // save()를 명시적으로 호출하지 않아도 DB에 반영되지만, 명시적으로 save()를 써도 무방합니다.
            users.save(user);
            log.info("Existing Kakao user updated. id={}", user.getId());
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
