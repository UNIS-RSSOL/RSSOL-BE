package com.example.unis_rssol.domain.schedule.generation.strategy;

import com.example.unis_rssol.domain.schedule.generation.ScheduleGenerationService.ScheduleSettingSnapshot;
import com.example.unis_rssol.domain.schedule.generation.dto.candidate.CandidateSchedule;
import com.example.unis_rssol.domain.schedule.workavailability.WorkAvailability;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 스케줄 생성 전략 인터페이스
 */
public interface ScheduleGenerationStrategy {

    /**
     * 후보 스케줄 생성
     *
     * @param storeId              매장 ID
     * @param settings             스케줄 설정 스냅샷
     * @param availabilities       근무 가능 시간 목록
     * @param userStoreUsernameMap userStoreId -> username 매핑
     * @param userStoreHireDateMap userStoreId -> hireDate 매핑
     * @return 생성된 후보 스케줄
     */
    CandidateSchedule generate(
            Long storeId,
            ScheduleSettingSnapshot settings,
            List<WorkAvailability> availabilities,
            Map<Long, String> userStoreUsernameMap,
            Map<Long, LocalDate> userStoreHireDateMap
    );

    /**
     * 전략 이름 반환
     */
    String getStrategyName();

    /**
     * 전략 설명 반환
     */
    String getDescription();
}

