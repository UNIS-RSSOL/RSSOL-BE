package com.example.unis_rssol.schedule.repository;

import com.example.unis_rssol.schedule.generation.entity.WorkShift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface WorkShiftRepository extends JpaRepository<WorkShift, Long> {

    // 겹침 조건: existing.start < newEnd && existing.end > newStart (1초라도 겹치면 true)
    boolean existsByUserStore_IdAndStartDatetimeLessThanAndEndDatetimeGreaterThan(
            Long userStoreId,
            LocalDateTime newEnd,
            LocalDateTime newStart
    );
  
    List<WorkShift> findByStore_Id(Long storeId);

    @Query("SELECT w FROM WorkShift w " +
            "WHERE w.store.id = :storeId " +
            "AND w.startDatetime BETWEEN :start AND :end " +
            "ORDER BY w.startDatetime ASC")
    List<WorkShift> findByStoreIdAndDateRange(
            @Param("storeId") Long storeId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("SELECT w FROM WorkShift w " +
            "WHERE w.userStore.user.id = :userId " +
            "AND w.startDatetime BETWEEN :start AND :end " +
            "ORDER BY w.startDatetime ASC")
    List<WorkShift> findMyShifts(
            @Param("userId") Long userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}
