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

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // 카카오 OAuth2 Redirect 콜백 - 프론트에서 받은 code를 백엔드가 처리

    @GetMapping("/kakao/callback")
    public void kakaoCallback(
            @RequestParam("code") String code,
            @RequestParam("redirect_uri") String redirectUri, // 프론트에서 전달
            HttpServletResponse response
    ) throws IOException {

        LoginResponse loginResponse = authService.handleKakaoCallback(code);

        // JWT를 쿼리 파라미터로 붙여서 프론트로 리다이렉트
        String targetUrl = redirectUri
                + "?accessToken=" + loginResponse.getAccessToken()
                + "&refreshToken=" + loginResponse.getRefreshToken()
                + "&userId=" + loginResponse.getUserId();

        response.sendRedirect(targetUrl);
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
