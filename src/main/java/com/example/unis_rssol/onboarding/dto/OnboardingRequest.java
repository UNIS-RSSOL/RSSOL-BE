package com.example.unis_rssol.onboarding.dto;

import lombok.Getter;

import java.time.LocalDate;

@Getter
public class OnboardingRequest {
    private String role;   // "OWNER" or "STAFF"

    // 공통 매장 필드
    private String storeCode; // STAFF일 경우 기존 매장 참여 코드
    private String name;      // OWNER일 경우 새 매장 이름
    private String address;   // OWNER일 경우 새 매장 주소
    private String phoneNumber;
    private String businessRegistrationNumber;

    // 은행 계좌 정보 (STAFF, OWNER 공통)
    private Integer bankId;
    private String accountNumber;

    // 알바생일 경우 입사날짜 작성
    private LocalDate hireDate;
}
