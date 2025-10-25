package com.example.unis_rssol.schedule.generation.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ConfirmScheduleRequestDto {
    private String candidateKey; // Redis 키
    private Integer index;           // 후보 스케줄 인덱스
    private LocalDate startDate;
    private LocalDate endDate;
}
