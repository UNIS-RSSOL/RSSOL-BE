package com.example.unis_rssol.domain.store.setting;

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

