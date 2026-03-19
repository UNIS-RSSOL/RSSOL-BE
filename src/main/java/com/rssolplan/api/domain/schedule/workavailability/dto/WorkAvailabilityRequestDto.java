package com.rssolplan.api.domain.schedule.workavailability.dto;

import com.rssolplan.api.domain.schedule.DayOfWeek;
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
        private DayOfWeek dayOfWeek;   // "MON", "TUE", ...
        private String startTime;   // "09:00:00"
        private String endTime;     // "15:00:00"
    }
}

