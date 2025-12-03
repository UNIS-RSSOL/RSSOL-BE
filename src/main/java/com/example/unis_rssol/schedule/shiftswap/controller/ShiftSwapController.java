package com.example.unis_rssol.schedule.shiftswap.controller;

import com.example.unis_rssol.schedule.shiftswap.dto.ShiftSwapManagerApprovalDto;
import com.example.unis_rssol.schedule.shiftswap.dto.ShiftSwapRequestCreateDto;
import com.example.unis_rssol.schedule.shiftswap.dto.ShiftSwapRespondDto;
import com.example.unis_rssol.schedule.shiftswap.dto.ShiftSwapResponseDto;
import com.example.unis_rssol.schedule.shiftswap.entity.Notification;
import com.example.unis_rssol.schedule.shiftswap.service.ShiftSwapService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/shift-swap")
public class ShiftSwapController {

    private final ShiftSwapService service;

    // 1. 대타 요청 생성
    @PostMapping("/requests")
    public ResponseEntity<List<ShiftSwapResponseDto>> create(
            @AuthenticationPrincipal Long userId,
            @RequestBody ShiftSwapRequestCreateDto dto) {
        List<ShiftSwapResponseDto> result = service.create(userId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }


    // 2. 수신자(알바/사장) 1차 응답
    @PatchMapping("/requests/{requestId}/respond")
    public ResponseEntity<ShiftSwapResponseDto> respond(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long requestId,
            @RequestBody ShiftSwapRespondDto dto) {
        return ResponseEntity.ok(service.respond(userId, requestId, dto));
    }

    // 3. 사장 최종 승인/거절
    @PatchMapping("/requests/{requestId}/manager-approval")
    public ResponseEntity<ShiftSwapResponseDto> managerApproval(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long requestId,
            @RequestBody ShiftSwapManagerApprovalDto dto) {
        return ResponseEntity.ok(service.managerApproval(userId, requestId, dto));
    }

    // 4. 알림 조회
    @GetMapping("/notifications")
    public ResponseEntity<List<Notification>> getNotifications(
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(service.getNotifications(userId));
    }

}
