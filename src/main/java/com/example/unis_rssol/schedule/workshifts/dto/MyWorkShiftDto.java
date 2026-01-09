package com.example.unis_rssol.schedule.workshifts.dto;

import com.example.unis_rssol.schedule.generation.entity.WorkShift;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class MyWorkShiftDto {
    private Long id;
    private Long storeId;    // userStore 객체 대신 ID만 담음
    private String storeName;
    private LocalDateTime startDatetime;
    private LocalDateTime endDatetime;
    private String shiftStatus;


    public MyWorkShiftDto(WorkShift ws) {
        this.id = ws.getId();
        this.startDatetime = ws.getStartDatetime();
        this.endDatetime = ws.getEndDatetime();
        this.shiftStatus = ws.getShiftStatus().name();
        this.storeId = ws.getUserStore() != null ? ws.getStore().getId() : null;
        this.storeName = ws.getUserStore() != null ? ws.getStore().getName() : null;
    }
}
