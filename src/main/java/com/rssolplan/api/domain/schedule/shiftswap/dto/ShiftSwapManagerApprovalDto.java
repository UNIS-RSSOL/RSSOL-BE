package com.rssolplan.api.domain.schedule.shiftswap.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ShiftSwapManagerApprovalDto {
    private String action; // "APPROVE" or "REJECT"
}
