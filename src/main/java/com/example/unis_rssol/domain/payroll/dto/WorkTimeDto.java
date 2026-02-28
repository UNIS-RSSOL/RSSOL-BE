package com.example.unis_rssol.domain.payroll.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * [생성 이유]
 * 근무 시간 정보를 수당 계산에 전달하기 위함.
 * <p>
 * [역할]
 * - 근무 시작/종료 시간, 휴일 여부 등의 정보 전달
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkTimeDto {

    private Long workShiftId;           // WorkShift ID
    private LocalDateTime startTime;    // 근무 시작 시간
    private LocalDateTime endTime;      // 근무 종료 시간
    private boolean isHoliday;          // 휴일 여부 (주휴일, 공휴일)
    private long breakMinutes;          // 휴게시간 (분)

    /**
     * WorkShift 엔티티로부터 DTO 생성
     */
    public static WorkTimeDto from(
            Long workShiftId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            boolean isHoliday,
            long breakMinutes
    ) {
        return WorkTimeDto.builder()
                .workShiftId(workShiftId)
                .startTime(startTime)
                .endTime(endTime)
                .isHoliday(isHoliday)
                .breakMinutes(breakMinutes)
                .build();
    }
}

