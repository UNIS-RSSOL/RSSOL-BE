package com.example.unis_rssol.global.auth.service.provider;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SocialProfile {
    private String provider;        // "kakao"
    private String providerId;      // 카카오 id
    private String username;        // 닉네임
    private String email;           // 이메일 (동의/검증된 경우)
    private String profileImageUrl; // 프로필 이미지

    private boolean isDefaultImage;
}
