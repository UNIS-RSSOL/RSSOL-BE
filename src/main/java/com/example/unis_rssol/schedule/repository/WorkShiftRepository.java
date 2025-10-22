package com.example.unis_rssol.schedule.repository;

import com.example.unis_rssol.schedule.entity.WorkShift;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkShiftRepository  extends JpaRepository<WorkShift, Long> {//eneity, PK 형식
}
