package com.example.unis_rssol.domain.payroll.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * [생성 이유]
 * 알바생이 본인이 근무하는 매장별 급여를 조회할 때 사용.
 * <p>
 * [역할]
 * - 매장 이름과 함께 해당 매장에서의 월별 급여 상세 제공
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffMyPayrollResponseDto {

    private Long storeId;               // 매장 ID
    private String storeName;           // 매장 이름
    private int year;                   // 조회 연도
    private int month;                  // 조회 월

    private int hourlyWage;             // 시급

    // 근무 시간 (분 단위)
    private long totalWorkMinutes;      // 총 근무 시간
    private long breakMinutes;          // 총 휴게 시간
    private long overtimeMinutes;       // 총 연장 근무 시간
    private long nightWorkMinutes;      // 총 야간 근무 시간
    private long holidayWorkMinutes;    // 총 휴일 근무 시간
    private long lateMinutes;           // 총 지각 시간 (분)

    // 수당 금액
    private BigDecimal basePay;         // 기본급
    private BigDecimal overtimePay;     // 연장수당
    private BigDecimal nightPay;        // 야간수당
    private BigDecimal holidayPay;      // 휴일수당
    private BigDecimal weeklyAllowance; // 주휴수당
    private BigDecimal totalPay;        // 총 급여

    // 근무 현황
    private int totalShiftCount;        // 총 근무 일수
    private int lateCount;              // 지각 횟수
    private int absenceCount;           // 결근 횟수
}

