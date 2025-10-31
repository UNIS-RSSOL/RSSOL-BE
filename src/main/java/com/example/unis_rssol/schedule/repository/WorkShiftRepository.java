package com.example.unis_rssol.schedule.repository;

import com.example.unis_rssol.schedule.entity.WorkShift;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface WorkShiftRepository extends JpaRepository<WorkShift, Long> {

    // 같은 user_store가 주어진 시간대(newStart~newEnd)와 '조금이라도' 겹치는 근무가 존재하는지 여부 / 겹침 조건: existing.start < newEnd && existing.end > newStart -> 1초라도 겹치면 true

    boolean existsByUserStore_IdAndStartDatetimeLessThanAndEndDatetimeGreaterThan(
            Long userStoreId,
            LocalDateTime newEnd,
            LocalDateTime newStart
    );
}
