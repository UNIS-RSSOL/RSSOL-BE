package com.example.unis_rssol.staffing.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StaffingRespondDto {
    private String action; // "ACCEPT" | "REJECT"
}
