package com.example.unis_rssol.schedule.generation.dto.candidate;

import com.example.unis_rssol.schedule.generation.dto.ShiftDto;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

// 후보별 시간표
@Getter @Setter
public class CandidateDto {
    private Integer candidateNumber;
    private List<ShiftDto> shifts = new ArrayList<>();
}
