package com.example.unis_rssol.shift.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ShiftSwapManagerApprovalDto {
    private String action; // "APPROVE" or "REJECT"
}
