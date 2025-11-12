package com.example.unis_rssol.staffing.repository;

import com.example.unis_rssol.staffing.entity.StaffingRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StaffingRequestRepository extends JpaRepository<StaffingRequest, Long> {
    List<StaffingRequest> findByStore_IdAndStatusOrderByCreatedAtDesc(Long storeId, StaffingRequest.Status status);
}
