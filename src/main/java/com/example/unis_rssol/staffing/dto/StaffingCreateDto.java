package com.example.unis_rssol.staffing.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StaffingCreateDto {
    private Long shiftId;     // 사장 UI에서 선택한 기존 work_shift의 id
    private int headcount;    // 필요한 인원 수
    private String note;      // 메모(선택)
}
