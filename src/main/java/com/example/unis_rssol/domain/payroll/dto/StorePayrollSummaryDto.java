package com.example.unis_rssol.domain.payroll.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 매장 전체 급여 요약 DTO (OWNER용)
 * - 매장 전체 인건비 합산
 * - 직원별 급여 목록 포함
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StorePayrollSummaryDto {

    private Long storeId;               // 매장 ID
    private String storeName;           // 매장 이름
    private int year;                   // 조회 연도
    private int month;                  // 조회 월

    // 직원 수
    private int totalEmployees;         // 총 직원 수

    // 합산 금액
    private BigDecimal totalRegularPay;     // 총 기본급
    private BigDecimal totalOvertimePay;    // 총 연장수당
    private BigDecimal totalNightPay;       // 총 야간수당
    private BigDecimal totalHolidayPay;     // 총 휴일수당
    private BigDecimal totalWeeklyHolidayPay; // 총 주휴수당
    private BigDecimal totalPay;            // 총 인건비

    // 직원별 급여 목록
    private List<EmployeePayrollDto> employees;
}

