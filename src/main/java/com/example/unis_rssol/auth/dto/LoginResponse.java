package com.example.unis_rssol.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter @AllArgsConstructor
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private Long userId;
    private boolean isNewUser;
    private String username;
    private String phoneNumber;
    private String provider;
    private String providerId;
}