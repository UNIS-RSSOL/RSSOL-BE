package com.rssolplan.api.domain.store.setting;

import lombok.*;

import java.time.LocalTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SegmentDto {
    private LocalTime startTime;
    private LocalTime endTime;
}

