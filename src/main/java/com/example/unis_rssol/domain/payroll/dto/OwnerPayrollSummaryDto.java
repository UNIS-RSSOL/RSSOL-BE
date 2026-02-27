package com.example.unis_rssol.domain.payroll.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * [생성 이유]
 * OWNER가 매장의 전체 직원 급여 현황을 조회할 때 사용.
 * <p>
 * [역할]
 * - 매장 정보와 함께 전체 직원들의 급여 합계 및 개별 상세 제공
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OwnerPayrollSummaryDto {

    private Long storeId;                       // 매장 ID
    private String storeName;                   // 매장 이름
    private int year;                           // 조회 연도
    private int month;                          // 조회 월

    // 전체 합계
    private int totalStaffCount;                // 총 직원 수
    private BigDecimal totalBasePay;            // 총 기본급 합계
    private BigDecimal totalOvertimePay;        // 총 연장수당 합계
    private BigDecimal totalNightPay;           // 총 야간수당 합계
    private BigDecimal totalHolidayPay;         // 총 휴일수당 합계
    private BigDecimal totalWeeklyAllowance;    // 총 주휴수당 합계
    private BigDecimal grandTotalPay;           // 총 급여 합계

    // 개별 직원 상세
    private List<StaffPayrollResponseDto> staffPayrolls;
}

