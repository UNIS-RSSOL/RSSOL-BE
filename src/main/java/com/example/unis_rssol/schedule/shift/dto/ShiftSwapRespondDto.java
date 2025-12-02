package com.example.unis_rssol.schedule.shift.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ShiftSwapRespondDto {
    private String action; // "ACCEPT" or "REJECT"
}
