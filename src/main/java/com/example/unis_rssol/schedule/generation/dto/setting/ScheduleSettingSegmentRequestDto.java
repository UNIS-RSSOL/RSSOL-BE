package com.example.unis_rssol.schedule.generation.dto.setting;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalTime;

@Getter
@Setter
public class ScheduleSettingSegmentRequestDto {
    private LocalTime startTime;
    private LocalTime endTime;
    private int requiredStaff;

    public ScheduleSettingSegmentRequestDto(LocalTime of, LocalTime of1, int i) {
    }
}
