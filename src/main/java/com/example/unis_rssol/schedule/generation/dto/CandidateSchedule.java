package com.example.unis_rssol.schedule.generation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Getter
@NoArgsConstructor
@AllArgsConstructor
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
