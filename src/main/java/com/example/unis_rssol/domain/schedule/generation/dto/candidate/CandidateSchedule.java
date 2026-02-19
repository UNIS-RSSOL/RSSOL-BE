package com.example.unis_rssol.domain.schedule.generation.dto.candidate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CandidateSchedule {
    private Long storeId;
    private List<CandidateShift> shifts = new ArrayList<>();

    // 사용된 전략 정보
    private String strategyName;
    private String strategyDescription;

    // 메타데이터 (비교용)
    private int totalShifts;           // 총 배정 수
    private int unassignedCount;       // 미배정(빈자리) 수
    private double coverageRate;       // 배정률 (%)

    public CandidateSchedule(Long storeId) {
        this.storeId = storeId;
    }

    public CandidateSchedule(Long storeId, String strategyName, String strategyDescription) {
        this.storeId = storeId;
        this.strategyName = strategyName;
        this.strategyDescription = strategyDescription;
    }

    public void addShift(CandidateShift shift) {
        this.shifts.add(shift);
    }

    /**
     * 메타데이터 계산
     */
    public void calculateMetadata() {
        this.totalShifts = shifts.size();
        this.unassignedCount = (int) shifts.stream()
                .filter(s -> "UNASSIGNED".equals(s.getStatus()))
                .count();
        int assignedCount = totalShifts - unassignedCount;
        this.coverageRate = totalShifts > 0
                ? Math.round((double) assignedCount / totalShifts * 100 * 10) / 10.0
                : 0;
    }
}
