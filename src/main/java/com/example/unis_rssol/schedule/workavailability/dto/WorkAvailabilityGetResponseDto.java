package com.example.unis_rssol.schedule.workavailability.dto;

import com.example.unis_rssol.schedule.workavailability.WorkAvailability;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkAvailabilityGetResponseDto {
    private String message;
    private long userStoreId;
    private List<AvailabilityItem> availabilities;

    @Getter
    @AllArgsConstructor
    public static class AvailabilityItem {
        private Long id;
        private WorkAvailability.DayOfWeek dayOfWeek;
        private String startTime;
        private String endTime;
    }
}
