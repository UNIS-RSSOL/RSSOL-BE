package com.example.unis_rssol.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter @AllArgsConstructor
public class RefreshTokenResponse {
    private String accessToken;
}