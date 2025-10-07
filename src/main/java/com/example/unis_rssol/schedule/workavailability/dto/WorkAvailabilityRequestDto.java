package com.example.unis_rssol.schedule.workavailability.dto;

import com.example.unis_rssol.schedule.workavailability.WorkAvailability;
import lombok.*;

import java.util.List;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkAvailabilityRequestDto {
    private List<AvailabilityItem> availabilities;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AvailabilityItem {
        private WorkAvailability.DayOfWeek dayOfWeek;   // "MON", "TUE", ...
        private String startTime;   // "09:00:00"
        private String endTime;     // "15:00:00"
    }
}

