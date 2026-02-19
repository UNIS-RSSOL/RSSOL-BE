package com.example.unis_rssol.domain.payroll.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 직원별 급여 상세 정보
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeePayrollDto {
    private Long userStoreId;
    private Long userId;
    private String username;
    private String storeName;

    // 시급 정보
    private Integer hourlyWage;        // 적용 시급

    // 근무 시간 (분 단위)
    private Integer totalWorkMinutes;  // 총 근무시간 (분)
    private Integer regularMinutes;    // 기본근무시간 (분)
    private Integer overtimeMinutes;   // 연장근무시간 (분) - 하루 8시간 초과
    private Integer nightMinutes;      // 야간근무시간 (분) - 22시~06시
    private Integer holidayMinutes;    // 휴일근무시간 (분)
    private Integer breakMinutes;      // 휴게시간 (분)

    // 급여 계산
    private BigDecimal regularPay;     // 기본급
    private BigDecimal overtimePay;    // 연장수당 (기본급의 50% 가산)
    private BigDecimal nightPay;       // 야간수당 (기본급의 50% 가산)
    private BigDecimal holidayPay;     // 휴일수당 (기본급의 50% 가산)
    private BigDecimal weeklyHolidayPay; // 주휴수당

    private BigDecimal totalPay;       // 총 급여

    // 근무 시간 (시간:분 형식)
    public String getTotalWorkTimeFormatted() {
        return formatMinutes(totalWorkMinutes);
    }

    public String getBreakTimeFormatted() {
        return formatMinutes(breakMinutes);
    }

    private String formatMinutes(Integer minutes) {
        if (minutes == null || minutes == 0) return "0시간 0분";
        int hours = minutes / 60;
        int mins = minutes % 60;
        return hours + "시간 " + mins + "분";
    }
}

