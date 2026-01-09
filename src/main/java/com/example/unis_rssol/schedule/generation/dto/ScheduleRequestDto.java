package com.example.unis_rssol.schedule.generation.dto;

import com.example.unis_rssol.schedule.generation.dto.candidate.GenerationOptionsDto;
import com.example.unis_rssol.schedule.generation.dto.setting.ScheduleSettingSegmentRequestDto;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
public class ScheduleRequestDto {

    private LocalTime openTime;
    private LocalTime closeTime;

    private LocalDate startDate;
    private LocalDate endDate;

    private List<ScheduleSettingSegmentRequestDto> timeSegments;
}

