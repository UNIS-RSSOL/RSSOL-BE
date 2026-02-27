package com.example.unis_rssol.domain.schedule.attendance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    Optional<Attendance> findByUserStoreIdAndWorkDate(
            Long userStoreId,
            LocalDate workDate
    );

    List<Attendance> findByUserStoreIdAndWorkDateBetween(
            Long userStoreId,
            LocalDate startDate,
            LocalDate endDate
    );

    /**
     * 특정 매장의 모든 직원 출석 기록 조회 (기간별)
     */
    @Query("SELECT a FROM Attendance a " +
            "WHERE a.userStoreId IN (SELECT us.id FROM UserStore us WHERE us.store.id = :storeId) " +
            "AND a.workDate BETWEEN :startDate AND :endDate")
    List<Attendance> findByStoreIdAndWorkDateBetween(
            @Param("storeId") Long storeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * 특정 WorkShift에 대한 출석 기록 조회
     */
    Optional<Attendance> findByWorkShiftId(Long workShiftId);
}
