package com.rssolplan.api.domain.schedule.workavailability;

import com.rssolplan.api.domain.schedule.DayOfWeek;
import com.rssolplan.api.domain.store.UserStore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface WorkAvailabilityRepository extends JpaRepository<WorkAvailability, Long> {
    Optional<WorkAvailability> findByUserStoreAndDayOfWeekAndStartTimeAndEndTime(
            UserStore userStore, DayOfWeek dayOfWeek,
            LocalTime startTime, LocalTime endTime);

    List<WorkAvailability> findByUserStore(UserStore userStore);

    List<WorkAvailability> findByUserStore_Store_Id(Long storeId);

    @Query("""
    select distinct wa.userStore.id
    from WorkAvailability wa
    where wa.userStore.store.id = :storeId
""")
    List<Long> findDistinctUserStoreIdsByStoreId(Long storeId);
}