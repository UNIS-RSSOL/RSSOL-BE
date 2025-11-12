package com.example.unis_rssol.schedule.workshifts.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WorkShiftCreateDto {
    private Long userStoreId;              // 근무자 (UserStore)
    private LocalDateTime startDatetime;   // 시작 시간
    private LocalDateTime endDatetime;     // 종료 시간
}