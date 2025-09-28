package com.example.unis_rssol.mypage.dto;

import lombok.*;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class OwnerProfileUpdateRequest {
    private String username;                   // 닉네임
    private String email;                      // 이메일
    private String businessRegistrationNumber; // 활성 매장 기준 사업자등록번호 수정
}
