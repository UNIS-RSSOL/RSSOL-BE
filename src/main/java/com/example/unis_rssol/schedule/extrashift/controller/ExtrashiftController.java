package com.example.unis_rssol.schedule.extrashift.controller;

import com.example.unis_rssol.schedule.extrashift.dto.*;
import com.example.unis_rssol.schedule.extrashift.service.ExtrashiftService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/extra-shift")
@RequiredArgsConstructor
public class ExtrashiftController {

    private final ExtrashiftService service;

    // 1. 사장님 추가 인력 요청 생성
    @PostMapping("/requests")
    public ResponseEntity<ExtrashiftRequestDetailDto> create(
            @AuthenticationPrincipal Long userId,
            @RequestBody ExtrashiftCreateDto dto
    ) {
        return ResponseEntity.status(201).body(service.create(userId, dto));
    }

    // 2. 알바생 수락/거절 1차 응답
    @PatchMapping("/requests/{requestId}/respond")
    public ResponseEntity<ExtrashiftResponseDetailDto> respond(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long requestId,
            @RequestBody ExtrashiftRespondDto dto
    ) {
        return ResponseEntity.ok(service.respond(userId, requestId, dto));
    }

    // 3. 사장님 최종 승인/거절 응답
    @PatchMapping("/requests/{requestId}/manager-approval")
    public ResponseEntity<ExtrashiftManagerApprovalDetailDto> managerApproval(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long requestId,
            @RequestBody ExtrashiftManagerApprovalDto dto
    ) {
        return ResponseEntity.ok(service.managerApproval(userId, requestId, dto));
    }
}
