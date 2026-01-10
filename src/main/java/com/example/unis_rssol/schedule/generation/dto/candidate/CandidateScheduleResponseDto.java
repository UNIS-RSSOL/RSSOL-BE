package com.example.unis_rssol.schedule.generation.dto.candidate;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

// 최종 API 응답
@Getter
@Setter
public class CandidateScheduleResponseDto {
    private String status = "success";
    private Map<Integer, CandidateDto> candidates = new HashMap<>();
    private Integer selectedCandidate; // 선택된 후보 번호
}

