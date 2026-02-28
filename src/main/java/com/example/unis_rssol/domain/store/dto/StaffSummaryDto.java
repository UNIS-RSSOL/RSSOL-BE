package com.example.unis_rssol.domain.store.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 직원 관리용 개별 직원 요약 정보
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffSummaryDto {
    private Long userStoreId;           //직원-매장 매핑 id
    private String username;            // 직원 이름
    private String profileImageUrl;     // 프로필 사진 URL
    private String role;                // 역할 (OWNER, STAFF 등)
    private String employmentStatus;    // 고용 상태 (HIRED, ON_LEAVE, RESIGNED)

    // 이번 달 급여 정보
    private BigDecimal monthlyPay;      // 이번 달 총 급여

    // 연락처 정보
    private String email;               // 이메일
    private String tel;                 // 전화번호 (현재 미지원 시 null)

    // 계좌 정보
    private String bankName;            // 은행명
    private String accountNumber;       // 계좌번호

    // 출결 정보 (이번 달)
    private int lateCount;              // 지각 횟수
    private int absenceCount;           // 결근 횟수
    private int totalShiftCount;        // 총 근무 일수
}

