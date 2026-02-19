package com.example.unis_rssol.domain.payroll.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * OWNER용 - 매장 전체 급여 요약
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StorePayrollSummaryDto {
    private Long storeId;
    private String storeName;
    private Integer year;
    private Integer month;

    // 전체 직원 수
    private int totalEmployees;

    // 합계
    private BigDecimal totalRegularPay;     // 총 기본급
    private BigDecimal totalOvertimePay;    // 총 연장수당
    private BigDecimal totalNightPay;       // 총 야간수당
    private BigDecimal totalHolidayPay;     // 총 휴일수당
    private BigDecimal totalWeeklyHolidayPay; // 총 주휴수당
    private BigDecimal totalPay;            // 총 급여

    // 직원별 상세 목록
    private List<EmployeePayrollDto> employees;
}

