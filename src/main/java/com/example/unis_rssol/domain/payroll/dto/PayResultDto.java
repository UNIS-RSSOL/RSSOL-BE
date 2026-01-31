package com.example.unis_rssol.domain.payroll.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * [생성 이유]
 * 급여 계산 결과를 종합적으로 반환하기 위함.
 * <p>
 * [역할]
 * - 특정 기간의 총 급여 계산 결과 전달
 * - 일별 상세 내역 포함 가능
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayResultDto {

    private Long userStoreId;                  // 근무자 매장 매핑 ID
    private String employeeName;               // 근무자 이름
    private LocalDate periodStart;             // 계산 기간 시작일
    private LocalDate periodEnd;               // 계산 기간 종료일

    private int hourlyWage;                    // 시급
    private boolean isFiveOrMoreEmployees;     // 5인 이상 사업장 여부

    // 총 근무 시간 요약 (분 단위)
    private long totalWorkMinutes;             // 총 근무 시간
    private long totalOvertimeMinutes;         // 총 연장 근무 시간
    private long totalNightMinutes;            // 총 야간 근무 시간
    private long totalHolidayMinutes;          // 총 휴일 근무 시간

    // 총 급여 요약
    private BigDecimal totalBasePay;           // 총 기본급
    private BigDecimal totalOvertimePay;       // 총 연장수당
    private BigDecimal totalNightPay;          // 총 야간수당
    private BigDecimal totalHolidayPay;        // 총 휴일수당
    private BigDecimal grandTotalPay;          // 총 급여

    // 일별 상세 내역
    private List<PayDetailDto> dailyDetails;   // 일별 상세 수당 내역
}

