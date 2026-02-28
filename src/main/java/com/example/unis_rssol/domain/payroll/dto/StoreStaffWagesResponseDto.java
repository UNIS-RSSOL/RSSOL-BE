package com.example.unis_rssol.domain.payroll.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 매장 전체 직원 시급 목록 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreStaffWagesResponseDto {
    private Long storeId;
    private String storeName;
    private Integer currentMinimumWage;    // 현재 최저임금
    private List<StaffWageInfoDto> staffWages;
}

