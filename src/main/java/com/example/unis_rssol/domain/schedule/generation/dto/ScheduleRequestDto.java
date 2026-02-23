package com.example.unis_rssol.domain.schedule.generation.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ScheduleRequestDto {
    private LocalDate startDate;
    private LocalDate endDate;

    // 시간대별 필요 인원수 설정
    private StaffRequirementDto staffRequirement;
}

