package com.example.unis_rssol.domain.store.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 매장 전체 직원 요약 목록 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AllStaffSummaryResponseDto {

    private Long storeId;               // 매장 ID
    private String storeName;           // 매장명
    private int year;                   // 조회 연도
    private int month;                  // 조회 월
    private int totalStaffCount;        // 총 직원 수

    private List<StaffSummaryDto> staffList;  // 직원 요약 목록
}

