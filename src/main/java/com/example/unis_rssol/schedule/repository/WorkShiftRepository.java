package com.example.unis_rssol.schedule.repository;

import com.example.unis_rssol.schedule.entity.ScheduleSettings;
import com.example.unis_rssol.schedule.entity.WorkShift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface WorkShiftRepository  extends JpaRepository<WorkShift, Long> {//eneity, PK 형식
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

}
