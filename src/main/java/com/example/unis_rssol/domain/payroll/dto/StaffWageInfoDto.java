package com.example.unis_rssol.domain.payroll.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 직원 시급 정보 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffWageInfoDto {
    private Long userStoreId;
    private String staffName;
    private Integer hourlyWage;        // null이면 최저임금 적용
    private Integer effectiveWage;     // 실제 적용 시급 (hourlyWage가 null이면 최저임금)
}
