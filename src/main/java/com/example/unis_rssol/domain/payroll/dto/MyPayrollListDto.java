package com.example.unis_rssol.domain.payroll.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 내가 속한 모든 매장의 급여 목록 DTO (STAFF용)
 * - userId 기준으로 본인이 속한 모든 매장의 급여 조회
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyPayrollListDto {

    private Long userId;                // User ID
    private String username;            // 사용자 이름
    private String profileImageUrl;     // 프로필 이미지 URL
    private int year;                   // 조회 연도
    private int month;                  // 조회 월

    // 전체 합산 (모든 매장 합산)
    private BigDecimal grandTotalPay;   // 모든 매장 급여 합계

    // 매장별 급여 목록
    private List<EmployeePayrollDto> payrolls;
}

