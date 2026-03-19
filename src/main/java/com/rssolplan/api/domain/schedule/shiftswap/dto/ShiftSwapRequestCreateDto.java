package com.rssolplan.api.domain.schedule.shiftswap.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ShiftSwapRequestCreateDto {
    private Long shiftId;
    private String reason;
}
