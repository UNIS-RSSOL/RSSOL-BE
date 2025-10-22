package com.example.unis_rssol.schedule.generation.dto;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class CandidateSchedule {
    private Long storeId;
    private List<CandidateShift> shifts = new ArrayList<>();

    public CandidateSchedule(Long storeId) {
        this.storeId = storeId;
    }

    public void addShift(CandidateShift shift) {
        this.shifts.add(shift);
    }


}
