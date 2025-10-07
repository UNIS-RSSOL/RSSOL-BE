package com.example.unis_rssol.schedule.workavailability.dto;

import com.example.unis_rssol.schedule.workavailability.WorkAvailability;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

// ✅ 매장 전체 직원의 근무 가능 시간 조회용
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class WorkAvailabilityAllResponseDto {

    private Long userStoreId;
    private String userName;
    private List<AvailabilityItem> availabilities;

    @Getter
    @AllArgsConstructor
    public static class AvailabilityItem {
        private WorkAvailability.DayOfWeek dayOfWeek;
        private String startTime;
        private String endTime;
    }
}

