package com.rssolplan.api.domain.schedule.generation;

import com.rssolplan.api.domain.schedule.generation.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleRepository extends JpaRepository<Schedule, Integer> {
}
