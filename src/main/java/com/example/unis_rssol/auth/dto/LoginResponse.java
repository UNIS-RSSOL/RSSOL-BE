package com.example.unis_rssol.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private Long userId;
    private boolean isNewUser;

    private String username;        // 닉네임
    private String email;           // 이메일
    private String profileImageUrl; // 프로필 이미지

    private String provider;
    private String providerId;

    private Long activeStoreId;
}
