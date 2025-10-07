package com.example.unis_rssol.onboarding.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class OnboardingResponse {
    private Long userId;
    private Long userStoreId;
    private Long storeId;
    private String position;         // OWNER or STAFF
    private String employmentStatus; // 항상 HIRED 기본
    private String storeCode;
    private String storeName;
    private String address;
    private String phoneNumber;
    private String businessRegistrationNumber;
    private Integer bankId;
    private String bankName;
    private String accountNumber;
    private LocalDate hireDate;
}
