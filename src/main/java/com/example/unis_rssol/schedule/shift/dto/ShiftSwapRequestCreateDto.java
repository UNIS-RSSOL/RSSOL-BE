package com.example.unis_rssol.schedule.shift.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ShiftSwapRequestCreateDto {
    private Long shiftId;
    private String reason;
}
