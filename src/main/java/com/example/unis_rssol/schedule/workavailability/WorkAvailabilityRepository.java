package com.example.unis_rssol.schedule.workavailability;

import com.example.unis_rssol.store.entity.UserStore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface WorkAvailabilityRepository extends JpaRepository<WorkAvailability, Long> {

    Optional<WorkAvailability> findByUserStoreAndDayOfWeekAndStartTimeAndEndTime(
            UserStore userStore,
            WorkAvailability.DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime
    );

    List<WorkAvailability> findByUserStore(UserStore userStore);

    List<WorkAvailability> findByUserStore_Store_Id(Long storeId);

    // 추가: 특정 요일(dayOfWeek) + 시간대 겹침 필터링 -> 겹치지 않는 사람에게 대타 요청 자동 발송
    @Query("""
        SELECT wa FROM WorkAvailability wa
        WHERE wa.userStore.store.id = :storeId
          AND wa.dayOfWeek = :dayOfWeek
          AND wa.startTime < :endTime
          AND wa.endTime > :startTime
    """)
    List<WorkAvailability> findOverlappingAvailabilities(
            @Param("storeId") Long storeId,
            @Param("dayOfWeek") WorkAvailability.DayOfWeek dayOfWeek,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime
    );
}
