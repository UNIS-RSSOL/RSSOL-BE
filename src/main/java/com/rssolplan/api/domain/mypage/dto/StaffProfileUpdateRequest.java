package com.rssolplan.api.domain.mypage.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class StaffProfileUpdateRequest {
    private String username;       // 닉네임
    private String email;          // 이메일 (수정 가능)
    private Integer bankId;        // 은행 변경
    private String accountNumber;  // 계좌 변경
}
