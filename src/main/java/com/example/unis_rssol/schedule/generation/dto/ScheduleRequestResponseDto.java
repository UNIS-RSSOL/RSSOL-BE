package com.example.unis_rssol.schedule.generation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ScheduleRequestResponseDto {

    private Long scheduleSettingId;
    private String status; // REQUESTED
}

