package com.example.unis_rssol.staffing.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StaffingManagerApprovalDto {
    private Long responseId; // 어떤 알바의 응답을 승인/거절하는가
    private String action;   // "APPROVE" | "REJECT"
}
