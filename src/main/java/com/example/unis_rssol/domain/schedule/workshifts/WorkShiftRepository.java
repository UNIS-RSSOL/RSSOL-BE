package com.example.unis_rssol.domain.schedule.workshifts;

import com.example.unis_rssol.domain.schedule.generation.entity.WorkShift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    @Query("""
            SELECT w FROM WorkShift w
            WHERE w.userStore.id = :userStoreId
            AND w.startDatetime < :end
            AND w.endDatetime > :start
            ORDER BY w.startDatetime ASC
            """)
    List<WorkShift> findShiftsOverlappingToday(
            @Param("userStoreId") Long userStoreId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Modifying
    @Query("DELETE FROM WorkShift ws " +
            "WHERE ws.store.id = :storeId " +
            "AND ws.startDatetime <= :end " +
            "AND ws.endDatetime >= :start")
    void deleteOverlappingShifts(
            @Param("storeId") Long storeId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    /**
     * 특정 매장의 특정 기간 내 모든 WorkShift 조회 (급여 계산용)
     */
    @Query("SELECT w FROM WorkShift w " +
            "WHERE w.store.id = :storeId " +
            "AND w.startDatetime >= :start " +
            "AND w.startDatetime < :end " +
            "ORDER BY w.userStore.id, w.startDatetime ASC")
    List<WorkShift> findByStoreIdAndMonthRange(
            @Param("storeId") Long storeId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    /**
     * 특정 UserStore의 특정 기간 내 모든 WorkShift 조회 (개인 급여 계산용)
     */
    @Query("SELECT w FROM WorkShift w " +
            "WHERE w.userStore.id = :userStoreId " +
            "AND w.startDatetime >= :start " +
            "AND w.startDatetime < :end " +
            "ORDER BY w.startDatetime ASC")
    List<WorkShift> findByUserStoreIdAndMonthRange(
            @Param("userStoreId") Long userStoreId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}
