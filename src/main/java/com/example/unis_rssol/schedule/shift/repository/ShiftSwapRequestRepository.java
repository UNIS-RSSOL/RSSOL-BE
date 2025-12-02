package com.example.unis_rssol.schedule.shift.repository;

import com.example.unis_rssol.schedule.shift.entity.ShiftSwapRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

public interface ShiftSwapRequestRepository extends JpaRepository<ShiftSwapRequest, Long> {

    // 같은 shiftId + receiverId 조합에 대해 진행 중(PENDING/ACCEPTED) 요청이 이미 있으면 true

    boolean existsByShift_IdAndReceiver_IdAndStatusIn(
            Long shiftId,
            Long receiverId,
            Collection<ShiftSwapRequest.Status> statuses
    );
}
