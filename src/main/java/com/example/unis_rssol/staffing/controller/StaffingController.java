package com.example.unis_rssol.staffing.controller;

import com.example.unis_rssol.staffing.dto.*;
import com.example.unis_rssol.staffing.service.StaffingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/staffing")
@RequiredArgsConstructor
public class StaffingController {

    private final StaffingService service;

    // 1. 사장님 추가 인력 요청 생성
    @PostMapping("/requests")
    public ResponseEntity<StaffingRequestDetailDto> create(
            @AuthenticationPrincipal Long userId,
            @RequestBody StaffingCreateDto dto
    ) {
        return ResponseEntity.status(201).body(service.create(userId, dto));
    }

    // 2. 알바생 수락/거절 1차 응답
    @PatchMapping("/requests/{requestId}/respond")
    public ResponseEntity<StaffingResponseDetailDto> respond(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long requestId,
            @RequestBody StaffingRespondDto dto
    ) {
        return ResponseEntity.ok(service.respond(userId, requestId, dto));
    }

    // 3. 사장님 최종 승인/거절 응답
    @PatchMapping("/requests/{requestId}/manager-approval")
    public ResponseEntity<StaffingManagerApprovalDetailDto> managerApproval(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long requestId,
            @RequestBody StaffingManagerApprovalDto dto
    ) {
        return ResponseEntity.ok(service.managerApproval(userId, requestId, dto));
    }
}
