package com.example.unis_rssol.schedule.generation.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

// shift 정보
@Getter @Setter
public class ShiftDto {
    private Long userStoreId;
    private LocalDateTime startDatetime;
    private LocalDateTime endDatetime;
}
