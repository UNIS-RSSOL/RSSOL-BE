package com.example.unis_rssol.schedule.repository;

import com.example.unis_rssol.schedule.entity.ScheduleSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ScheduleSettingsRepository extends JpaRepository<ScheduleSettings, Long> {
    Optional<ScheduleSettings> findByStoreId(Long storeId);
}
