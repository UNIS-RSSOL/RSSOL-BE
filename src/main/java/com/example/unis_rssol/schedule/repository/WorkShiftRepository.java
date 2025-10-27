package com.example.unis_rssol.schedule.repository;

import com.example.unis_rssol.schedule.entity.WorkShift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface WorkShiftRepository extends JpaRepository<WorkShift, Long> {

    // 특정 매장에서 지정한 시간대에 이미 근무가 잡힌 직원(user_store.id) ID 목록 조회→ 대타 요청 시 제외해야 하는 "바쁜 사람"만 반환

    @Query("""
        SELECT ws.userStore.id
        FROM WorkShift ws
        WHERE ws.userStore.store.id = :storeId
          AND ws.shiftStatus <> com.example.unis_rssol.schedule.entity.WorkShift$ShiftStatus.CANCELED
          AND ws.startDatetime < :end
          AND ws.endDatetime > :start
    """)
    List<Long> findBusyUserStoreIds(
            @Param("storeId") Long storeId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}
