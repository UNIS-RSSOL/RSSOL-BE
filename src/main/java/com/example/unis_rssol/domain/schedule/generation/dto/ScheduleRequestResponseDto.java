package com.example.unis_rssol.domain.schedule.generation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
@Builder
public class ScheduleRequestResponseDto {

    private Long scheduleRequestId;  // ScheduleRequest의 ID
    private Long storeId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status; // REQUESTED, GENERATED, CONFIRMED
}

