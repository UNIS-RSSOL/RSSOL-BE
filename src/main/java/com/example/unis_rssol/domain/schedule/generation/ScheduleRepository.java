package com.example.unis_rssol.domain.schedule.generation;

import com.example.unis_rssol.domain.schedule.generation.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleRepository extends JpaRepository<Schedule, Integer> {
}
