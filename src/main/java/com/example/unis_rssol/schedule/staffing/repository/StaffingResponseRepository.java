package com.example.unis_rssol.schedule.staffing.repository;

import com.example.unis_rssol.schedule.staffing.entity.StaffingResponse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StaffingResponseRepository extends JpaRepository<StaffingResponse, Long> {

    // 중복 응답 방지용
    boolean existsByStaffingRequest_IdAndCandidate_Id(Long staffingRequestId, Long candidateUserStoreId);

    // 매니저 승인 대기중(또는 전체) 응답들 조회용(필요시)
    List<StaffingResponse> findByStaffingRequest_Id(Long staffingRequestId);
}
