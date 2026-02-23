package com.example.unis_rssol.domain.schedule.generation.dto.setting;

import lombok.*;

/**
 * 세그먼트별 필요 인원수 요청 DTO
 * - 시간대는 StoreSetting에서 가져오고, 인원수만 별도로 받음
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleSettingSegmentRequestDto {
    private int segmentIndex;  // 세그먼트 순서 (0부터 시작)
    private int requiredStaff; // 해당 세그먼트 필요 인원수
}
