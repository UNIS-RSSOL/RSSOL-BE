package com.example.unis_rssol.auth.controller;

import com.example.unis_rssol.auth.dto.LoginResponse;
import com.example.unis_rssol.auth.dto.RefreshTokenResponse;
import com.example.unis_rssol.auth.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // 프론트에서 토큰을 받을 페이지 (원하는 경로로 바꿔도 됨)
    // 예: https://rssol-fe.vercel.app/login/kakao-result
    private static final String KAKAO_FRONTEND_REDIRECT =
            "https://rssol-fe.vercel.app/login/kakao-result";

    /**
     * 카카오 OAuth2 Redirect 콜백 (카카오 → 백엔드)
     * - 카카오가 code를 붙여서 이 URL로 리다이렉트
     * - 백엔드가 code로 카카오 토큰/프로필 처리 + 우리 JWT 발급
     * - 다시 프론트로 리다이렉트하면서 accessToken/refreshToken을 쿼리로 전달
     */
    @GetMapping("/kakao/callback")
    public void kakaoCallback(@RequestParam("code") String code,
                              HttpServletResponse response) throws IOException {

        // 기존에 쓰던 서비스 그대로 재사용
        LoginResponse login = authService.handleKakaoCallback(code);

        // 프론트로 넘길 값들
        String accessToken  = login.getAccessToken();
        String refreshToken = login.getRefreshToken();
        boolean isNewUser   = login.isNewUser();
        Long userId         = login.getUserId();

        // URL 인코딩 (토큰에 점(.) 등 특수문자 있어서 꼭 인코딩)
        String redirectUrl = KAKAO_FRONTEND_REDIRECT
                + "?accessToken=" + URLEncoder.encode(accessToken, StandardCharsets.UTF_8)
                + "&refreshToken=" + URLEncoder.encode(refreshToken, StandardCharsets.UTF_8)
                + "&isNewUser=" + isNewUser
                + "&userId=" + userId;

        // 프론트로 리다이렉트
        response.sendRedirect(redirectUrl);
    }

    // Refresh Token으로 Access Token 재발급
    @PostMapping("/refresh-token")
    public ResponseEntity<RefreshTokenResponse> refresh(@RequestHeader("Authorization") String bearer) {
        String refreshToken = bearer.replace("Bearer ", "");
        return ResponseEntity.ok(authService.refresh(refreshToken));
    }

    // 로그아웃
    @PostMapping("/logout")
    public ResponseEntity<String> logout(@AuthenticationPrincipal Long userId) {
        authService.logout(userId);
        return ResponseEntity.ok("로그아웃 성공");
    }
}
