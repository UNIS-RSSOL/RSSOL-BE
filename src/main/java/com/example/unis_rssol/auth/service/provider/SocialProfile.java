package com.example.unis_rssol.auth.service.provider;

import lombok.*;

@Getter @AllArgsConstructor
public class SocialProfile {
    private final String provider;       // kakao
    private final String providerId;     // 카카오 회원 고유 id
    private final String username;       // 닉네임
    private final String email;          // 이메일
    private final String profileImageUrl;// 프로필 사진 URL
}
