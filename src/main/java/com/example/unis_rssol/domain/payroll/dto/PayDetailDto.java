package com.example.unis_rssol.domain.payroll.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * [생성 이유]
 * 수당 계산 결과를 항목별로 상세하게 전달하기 위함.
 * <p>
 * [역할]
 * - 기본급, 연장수당, 야간수당, 휴일수당 등 항목별 수당 상세 정보 전달
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayDetailDto {

    // 기본 정보
    private int hourlyWage;                    // 시급
    private boolean isFiveOrMoreEmployees;     // 5인 이상 사업장 여부

    // 근무 시간 (분 단위)
    private long totalWorkMinutes;             // 총 근무 시간
    private long standardWorkMinutes;          // 기본 근무 시간 (8시간 이내)
    private long overtimeMinutes;              // 연장 근무 시간
    private long nightWorkMinutes;             // 야간 근무 시간
    private long holidayWorkMinutes;           // 휴일 근무 시간
    private long holidayOvertimeMinutes;       // 휴일 초과 근무 시간 (8시간 초과분)

    // 중복 가산 시간 (분 단위)
    private long overtimeNightMinutes;         // 연장 + 야간 중복 시간
    private long holidayNightMinutes;          // 휴일 + 야간 중복 시간
    private long holidayOvertimeNightMinutes;  // 휴일초과 + 야간 중복 시간

    // 수당 금액
    private BigDecimal basePay;                // 기본급
    private BigDecimal overtimePay;            // 연장수당
    private BigDecimal nightPay;               // 야간수당
    private BigDecimal holidayPay;             // 휴일수당
    private BigDecimal totalPay;               // 총 급여

    // 적용된 배율 정보 (디버깅/확인용)
    private String appliedRatesDescription;    // 적용된 배율 설명
}

