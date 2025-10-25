package com.example.unis_rssol.schedule.generation.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ScheduleGenerationResponseDto {
    private String status;                     // "success" or "error"
    private Long scheduleSettingsId;           // DB에 저장된 설정 ID
    private Long storeId;
    private List<ScheduleSettingSegmentResponseDto> timeSegments; // 저장된 time slot 정보
    private String candidateScheduleKey;       // Redis key
    private int generatedCount;                // 생성된 후보 스케줄 개수
    private List<Long> unsubmittedEmployeeIds; // 미제출한 직원들을 따로 보여주도록하자
}
