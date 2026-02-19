package com.example.unis_rssol.domain.payroll.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * STAFF용 - 본인의 모든 매장 급여 목록
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyPayrollListDto {
    private Long userId;
    private String username;
    private Integer year;
    private Integer month;

    // 매장별 급여 목록
    private List<EmployeePayrollDto> payrolls;
}

