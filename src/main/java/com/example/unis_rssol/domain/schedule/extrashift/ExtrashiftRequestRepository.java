package com.example.unis_rssol.domain.schedule.extrashift;

import com.example.unis_rssol.domain.schedule.extrashift.entity.ExtrashiftRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExtrashiftRequestRepository extends JpaRepository<ExtrashiftRequest, Long> {
    List<ExtrashiftRequest> findByStore_IdAndStatusOrderByCreatedAtDesc(
            Long storeId,
            ExtrashiftRequest.Status status
    );
}
