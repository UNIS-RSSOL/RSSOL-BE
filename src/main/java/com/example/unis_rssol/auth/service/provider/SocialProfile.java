package com.example.unis_rssol.auth.service.provider;

import lombok.*;

@Getter @AllArgsConstructor
public class SocialProfile {
    private final String provider;    // kakao|naver|google
    private final String providerId;  // external user id
    private final String username;    // name/nickname
    private final String phoneNumber; // may be null
}