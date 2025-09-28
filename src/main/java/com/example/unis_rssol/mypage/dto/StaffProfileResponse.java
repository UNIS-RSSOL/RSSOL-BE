package com.example.unis_rssol.mypage.dto;

import lombok.*;

@Getter @Setter @Builder
@AllArgsConstructor @NoArgsConstructor
public class StaffProfileResponse {
    private Long userId;
    private String username;
    private String email;
    private String profileImageUrl;
    private String position;           // STAFF
    private String employmentStatus;   // 활성 매장 기준

    private CurrentStore currentStore; // 활성 매장 간단 정보
    private BankAccount bankAccount;   // 대표 계좌

    @Getter @Setter @AllArgsConstructor @NoArgsConstructor @Builder
    public static class CurrentStore {
        private Long storeId;
        private String name;
        private String storeCode;
    }

    @Getter @Setter @AllArgsConstructor @NoArgsConstructor @Builder
    public static class BankAccount {
        private Integer bankId;
        private String bankName;
        private String accountNumber;
    }
}
