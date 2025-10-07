package com.example.unis_rssol.schedule.workavailability.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkAvailabilityPatchResponseDto {
    private Long availabilityId;  // DB상의 WorkAvailability PK
    private AvailabilityStatus status; // 문자열 대신 Enum      // "INSERTED", "UPDATED", "DELETED"
}
