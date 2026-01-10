package com.example.unis_rssol.schedule.generation.dto;

import com.example.unis_rssol.schedule.generation.dto.candidate.GenerationOptionsDto;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ScheduleGenerationRequestDto {
    private GenerationOptionsDto generationOptions; // 몇 개 후보 만들지
}
