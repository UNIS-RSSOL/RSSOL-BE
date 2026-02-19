package com.example.unis_rssol.domain.payroll.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 직원 시급 설정 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class StaffWageUpdateDto {

    @NotNull(message = "시급은 필수입니다")
    @Min(value = 0, message = "시급은 0 이상이어야 합니다")
    private Integer hourlyWage;
}

