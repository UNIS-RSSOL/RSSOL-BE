package com.example.unis_rssol.schedule.generation.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
public class ScheduleGenerationRequestDto {
    private Long storeId;
    private LocalTime openTime;
    private LocalTime closeTime;
    private LocalDate startDate; // 시작 날짜
    private LocalDate endDate;   // 종료 날짜 (n주 뒤)
    private List<ScheduleSettingSegmentRequestDto> timeSegments;
    private GenerationOptionsDto generationOptions;
}