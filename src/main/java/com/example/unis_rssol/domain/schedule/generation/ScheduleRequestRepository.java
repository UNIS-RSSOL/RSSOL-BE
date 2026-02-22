package com.example.unis_rssol.domain.schedule.generation;

import com.example.unis_rssol.domain.schedule.generation.entity.ScheduleRequest;
import com.example.unis_rssol.domain.schedule.generation.entity.ScheduleRequest.ScheduleRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ScheduleRequestRepository extends JpaRepository<ScheduleRequest, Long> {

    Optional<ScheduleRequest> findByStoreIdAndStatus(Long storeId, ScheduleRequestStatus status);

    List<ScheduleRequest> findByStoreIdOrderByCreatedAtDesc(Long storeId);

    Optional<ScheduleRequest> findByStoreIdAndStartDateAndEndDate(Long storeId, LocalDate startDate, LocalDate endDate);

    // 특정 기간과 겹치는 요청이 있는지 확인
    List<ScheduleRequest> findByStoreIdAndStatusIn(Long storeId, List<ScheduleRequestStatus> statuses);
}

