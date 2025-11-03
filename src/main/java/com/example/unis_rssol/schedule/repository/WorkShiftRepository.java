package com.example.unis_rssol.schedule.repository;

import com.example.unis_rssol.schedule.entity.WorkShift;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface WorkShiftRepository extends JpaRepository<WorkShift, Long> {

    // 겹침 조건: existing.start < newEnd && existing.end > newStart (1초라도 겹치면 true)
    boolean existsByUserStore_IdAndStartDatetimeLessThanAndEndDatetimeGreaterThan(
            Long userStoreId,
            LocalDateTime newEnd,
            LocalDateTime newStart
    );
}
