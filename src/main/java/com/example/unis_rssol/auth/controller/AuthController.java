package com.example.unis_rssol.auth.controller;

import com.example.unis_rssol.auth.dto.LoginResponse;
import com.example.unis_rssol.auth.dto.RefreshTokenResponse;
import com.example.unis_rssol.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // 카카오 OAuth2 Redirect 콜백 - 프론트에서 받은 code를 백엔드가 처리

    @GetMapping("/kakao/callback")
    public ResponseEntity<LoginResponse> kakaoCallback(@RequestParam("code") String code) {
        return ResponseEntity.ok(authService.handleKakaoCallback(code));
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
