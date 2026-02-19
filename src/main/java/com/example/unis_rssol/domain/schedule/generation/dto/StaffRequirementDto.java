package com.example.unis_rssol.domain.schedule.generation.dto;

import lombok.*;

import java.util.List;

/**
 * 스케줄 생성 시 시간대별 필요 인원수 설정 DTO
 * - 시간대는 StoreSetting에서 가져오고, 인원수만 별도로 받음
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffRequirementDto {

    // 세그먼트 사용 시: 각 세그먼트별 필요 인원수
    private List<SegmentStaffDto> segmentStaffList;

    // 세그먼트 미사용 시: 전체 시간대 필요 동시 근무자 수
    private Integer requiredStaff;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SegmentStaffDto {
        private int segmentIndex;  // 세그먼트 순서 (0부터 시작)
        private int requiredStaff; // 해당 세그먼트 필요 인원수
    }
}

