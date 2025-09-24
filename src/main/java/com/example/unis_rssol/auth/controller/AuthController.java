package com.example.unis_rssol.auth.controller;

import com.example.unis_rssol.auth.dto.LoginResponse;
import com.example.unis_rssol.auth.dto.RefreshTokenResponse;
import com.example.unis_rssol.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Google/Naver/Kakao OAuth2 Redirect 콜백
     */
    @GetMapping("/{provider}/callback")
    public ResponseEntity<LoginResponse> callback(
            @PathVariable String provider,
            @RequestParam("code") String code) {
        return ResponseEntity.ok(authService.handleCallback(provider, code));
    }

    /**
     * Refresh Token으로 Access Token 재발급
     */
    @PostMapping("/refresh-token")
    public ResponseEntity<RefreshTokenResponse> refresh(@RequestHeader("Authorization") String bearer) {
        String refreshToken = bearer.replace("Bearer ", "");
        return ResponseEntity.ok(authService.refresh(refreshToken));
    }
}
