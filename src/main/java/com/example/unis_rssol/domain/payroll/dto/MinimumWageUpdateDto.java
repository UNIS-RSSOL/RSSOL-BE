package com.example.unis_rssol.domain.payroll.dto;

import lombok.*;

import java.time.LocalDate;

/**
 * 최저임금 등록/수정 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MinimumWageUpdateDto {
    private Integer hourlyWage;       // 시급 (원)
    private LocalDate effectiveFrom;  // 적용 시작일
    private LocalDate effectiveTo;    // 적용 종료일 (null이면 현재 적용 중)
    private String description;       // 설명 (예: "2025년 최저임금")
}

