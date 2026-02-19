package com.example.unis_rssol.domain.schedule.generation.dto.candidate;

import lombok.Data;

@Data
public class ConfirmScheduleRequestDto {
    private Integer candidateIndex;  // 후보 스케줄 인덱스 (0부터 시작)
}
