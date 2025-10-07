package com.example.unis_rssol.schedule.workavailability.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkAvailabilityCreateResponseDto {
    private String message;
    private long userStoreId; // JWT기반 조회 user_store_id
    private int inserted;

}
