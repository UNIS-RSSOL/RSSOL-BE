package com.example.unis_rssol.schedule.workavailability;

import com.example.unis_rssol.store.entity.UserStore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface WorkAvailabilityRepository extends JpaRepository<WorkAvailability, Long> {
    Optional<WorkAvailability> findByUserStoreAndDayOfWeekAndStartTimeAndEndTime(
            UserStore userStore, WorkAvailability.DayOfWeek dayOfWeek,
            LocalTime startTime, LocalTime endTime);

    List<WorkAvailability> findByUserStore(UserStore userStore);

    List<WorkAvailability> findByUserStore_Store_Id(Long storeId);
}