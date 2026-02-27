package com.example.unis_rssol.domain.schedule.attendance;

import org.springframework.data.jpa.repository.JpaRepository;

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
}
