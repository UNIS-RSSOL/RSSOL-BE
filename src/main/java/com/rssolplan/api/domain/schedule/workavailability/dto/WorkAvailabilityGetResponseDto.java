package com.rssolplan.api.domain.schedule.workavailability.dto;

import com.rssolplan.api.domain.schedule.DayOfWeek;
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
        private DayOfWeek dayOfWeek;
        private String startTime;
        private String endTime;
    }
}
