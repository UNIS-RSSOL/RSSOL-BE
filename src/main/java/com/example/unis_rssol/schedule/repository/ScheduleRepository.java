package com.example.unis_rssol.schedule.repository;

import com.example.unis_rssol.schedule.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleRepository extends JpaRepository<Schedule, Integer> {
}
