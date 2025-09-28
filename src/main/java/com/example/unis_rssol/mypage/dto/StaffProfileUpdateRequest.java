package com.example.unis_rssol.mypage.dto;

import lombok.*;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class StaffProfileUpdateRequest {
    private String username;       // 닉네임
    private String email;          // 이메일 (수정 가능)
    private Integer bankId;        // 은행 변경
    private String accountNumber;  // 계좌 변경
}
