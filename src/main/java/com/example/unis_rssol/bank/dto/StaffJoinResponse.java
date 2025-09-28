package com.example.unis_rssol.bank.dto;

import lombok.Getter; import lombok.AllArgsConstructor;
@Getter @AllArgsConstructor
public class StaffJoinResponse {
    private Long userId;
    private Long userStoreId;
    private Long storeId;
    private String position; // STAFF
    private String employmentStatus;
    private String storeName;
    private String address;
    private String phoneNumber;
    private Integer bankId;
    private String bankName;
    private String accountNumber;
}