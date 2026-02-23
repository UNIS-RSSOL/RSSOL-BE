package com.example.unis_rssol.domain.store.setting;

import lombok.*;

import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreSettingDto {
    private LocalTime openTime;
    private LocalTime closeTime;

    // 세그먼트 사용 여부 (true: 파트타임 구간 나눔, false: 30분 단위 N명 배정)
    private boolean useSegments;

    // 세그먼트 사용 시
    private List<SegmentDto> segments;


    // 브레이크타임
    private boolean hasBreakTime;
    private LocalTime breakStartTime;
    private LocalTime breakEndTime;
}
