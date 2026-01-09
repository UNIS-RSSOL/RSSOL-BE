package com.example.unis_rssol.schedule.generation.dto.candidate;

import com.example.unis_rssol.schedule.DayOfWeek;
import lombok.*;

import java.time.LocalTime;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CandidateShift {
    private Long userStoreId;
    private LocalTime startTime;
    private LocalTime endTime;
    private DayOfWeek day;         // 배정 요일
    private String status = null;  // "UNASSIGNED"

    public CandidateShift(Long userStoreId, DayOfWeek day, LocalTime start, LocalTime end) {
        this.userStoreId = userStoreId;
        this.day = day;
        this.startTime = start;
        this.endTime = end;
        this.status = null; // 기본값
    }

    public CandidateShift(Long userStoreId, DayOfWeek day, LocalTime start, LocalTime end, String status) {
        this.userStoreId = userStoreId;
        this.day = day;
        this.startTime = start;
        this.endTime = end;
        this.status = status;
    }
}
