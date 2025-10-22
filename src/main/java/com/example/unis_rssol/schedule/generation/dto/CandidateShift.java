package com.example.unis_rssol.schedule.generation.dto;

import com.example.unis_rssol.schedule.DayOfWeek;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
@AllArgsConstructor
public class CandidateShift {
    private Long userStoreId;
    private LocalTime startTime;
    private LocalTime endTime;
    private DayOfWeek day;         // 배정 요일
    private String status = null;  // "UNASSIGNED"

    public CandidateShift(Long id, DayOfWeek day, LocalTime start, LocalTime end) {
    }

    public CandidateShift(Long userStoreId, DayOfWeek day, LocalTime start, LocalTime end, String unassigned) {
    }
}
