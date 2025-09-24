package com.example.unis_rssol.auth.service.provider;

public interface SocialProvider {
    boolean supports(String provider);
    String getAccessTokenFromCode(String code);
    SocialProfile fetchProfile(String accessToken);    // call provider API with Bearer token
}