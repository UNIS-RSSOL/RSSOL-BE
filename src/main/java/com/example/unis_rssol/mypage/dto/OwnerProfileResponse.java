package com.example.unis_rssol.mypage.dto;

import lombok.*;

@Getter @Setter @Builder
@AllArgsConstructor @NoArgsConstructor
public class OwnerProfileResponse {
    private Long userId;
    private String username;
    private String email;
    private String profileImageUrl;
    private String position;           // OWNER
    private String employmentStatus;   // 활성 매장 기준
    private String businessRegistrationNumber; // 활성 매장 기준
}
