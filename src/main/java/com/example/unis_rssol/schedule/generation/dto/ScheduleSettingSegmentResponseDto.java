package com.example.unis_rssol.schedule.generation.dto;

import lombok.*;

import java.time.LocalTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ScheduleSettingSegmentResponseDto {
    private Long id;
    private LocalTime startTime;
    private LocalTime endTime;
    private int requiredStaff;
}