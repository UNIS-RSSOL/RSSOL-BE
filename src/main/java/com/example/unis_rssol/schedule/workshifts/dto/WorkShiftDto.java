package com.example.unis_rssol.schedule.workshifts.dto;

import com.example.unis_rssol.schedule.entity.WorkShift;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class WorkShiftDto {
    private Long id;
    private Long userStoreId;    // userStore 객체 대신 ID만 담음
    private LocalDateTime startDatetime;
    private LocalDateTime endDatetime;
    private String shiftStatus;


    public WorkShiftDto(WorkShift ws) {
        this.id = ws.getId();
        this.startDatetime = ws.getStartDatetime();
        this.endDatetime = ws.getEndDatetime();
        this.shiftStatus = ws.getShiftStatus().name();
        this.userStoreId = ws.getUserStore() != null ? ws.getUserStore().getId() : null;
    }
}
