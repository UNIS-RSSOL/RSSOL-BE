package com.example.unis_rssol.domain.schedule.generation.dto.candidate;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class GenerationOptionsDto {
    /**
     * 생성할 후보 스케줄 개수 (기본값: 4, 각 전략별 1개씩)
     */
    private int candidateCount = 4;

    /**
     * 사용할 전략 목록 (null이면 모든 전략 사용)
     * - BALANCED: 경력자-신입 균형 배치
     * - COVERAGE_FIRST: 빈자리 최소화 우선
     * - SENIOR_PRIORITY: 경력자 우선 배치
     * - FAIR_DISTRIBUTION: 근무시간 공정 배분
     */
    private List<GenerationStrategy> strategies;

    public enum GenerationStrategy {
        BALANCED,           // 경력자-신입 균형 배치 (기본)
        COVERAGE_FIRST,     // 빈자리 최소화 우선
        SENIOR_PRIORITY,    // 경력자 우선
        FAIR_DISTRIBUTION   // 근무시간 공정 배분
    }
}
