package com.example.unis_rssol.domain.payroll.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 개별 직원의 월별 급여 상세 정보 DTO
 * - 특정 매장의 특정 직원 급여 상세 조회
 * - 내가 속한 매장별 급여 조회
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeePayrollDto {

    private Long userStoreId;           // UserStore ID
    private Long userId;                // User ID
    private String username;            // 직원 이름
    private String profileImageUrl;     // 프로필 이미지 URL
    private String storeName;           // 매장 이름
    private int hourlyWage;             // 시급

    // 근무 시간 (분 단위)
    private long totalWorkMinutes;      // 총 근무 시간 (휴게시간 제외)
    private long regularMinutes;        // 기본 근무 시간 (8시간 이내)
    private long overtimeMinutes;       // 연장 근무 시간 (8시간 초과)
    private long nightMinutes;          // 야간 근무 시간 (22:00~06:00)
    private long holidayMinutes;        // 휴일 근무 시간
    private long breakMinutes;          // 총 휴게 시간
    private long lateMinutes;           // 총 지각 시간

    // 수당 금액
    private BigDecimal regularPay;      // 기본급
    private BigDecimal overtimePay;     // 연장수당
    private BigDecimal nightPay;        // 야간수당
    private BigDecimal holidayPay;      // 휴일수당
    private BigDecimal weeklyHolidayPay;// 주휴수당
    private BigDecimal totalPay;        // 총 급여

    // 출결 정보
    private int totalShiftCount;        // 총 근무 일수
    private int lateCount;              // 지각 횟수
    private int absenceCount;           // 결근 횟수
}

