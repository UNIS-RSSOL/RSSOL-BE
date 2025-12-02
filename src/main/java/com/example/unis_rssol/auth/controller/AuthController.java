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

    @GetMapping("/kakao/callback")
    public void kakaoCallback(
            @RequestParam("code") String code,
            // ★ 중요: 프론트가 보낸 돌아갈 주소는 'state'에 담겨 옴
            @RequestParam(value = "state", required = false) String state,
            HttpServletResponse response
    ) throws IOException {

        LoginResponse loginResponse = authService.handleKakaoCallback(code);

        // 1. state 값이 있으면 그 주소(로컬 등) 사용, 없으면 배포 주소(기본값) 사용
        String redirectBaseUrl = (state != null && !state.isBlank())
                ? state
                : "https://rssol-fe.vercel.app/auth/kakao/callback";

        // 2. 프론트로 토큰을 붙여서 리다이렉트
        String targetUrl = redirectBaseUrl
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
