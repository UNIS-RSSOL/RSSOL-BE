package com.example.unis_rssol.domain.schedule.generation;

import com.example.unis_rssol.domain.schedule.generation.dto.ScheduleGenerationRequestDto;
import com.example.unis_rssol.domain.schedule.generation.dto.ScheduleGenerationResponseDto;
import com.example.unis_rssol.domain.schedule.generation.dto.ScheduleRequestDto;
import com.example.unis_rssol.domain.schedule.generation.dto.ScheduleRequestResponseDto;
import com.example.unis_rssol.domain.schedule.generation.dto.candidate.CandidateSchedule;
import com.example.unis_rssol.domain.schedule.generation.dto.candidate.ConfirmScheduleRequestDto;
import com.example.unis_rssol.domain.schedule.generation.entity.Schedule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@Slf4j
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
public class ScheduleGenerationController {
    private final ScheduleGenerationService service;

    /**
     * 1. 스케줄 요청 (알바생에게 근무 가능 시간 입력 요청)
     * - 시간대별 필요 인원수도 함께 전송
     */
    @PostMapping("/requests")
    public ResponseEntity<ScheduleRequestResponseDto> requestSchedule(
            @AuthenticationPrincipal Long userId,
            @RequestBody ScheduleRequestDto request) {
        ScheduleRequestResponseDto response = service.requestSchedule(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 2. 스케줄 생성 (후보군 생성)
     */
    @PostMapping("/requests/{scheduleRequestId}/generate")
    public ResponseEntity<ScheduleGenerationResponseDto> generateSchedule(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long scheduleRequestId,
            @RequestBody ScheduleGenerationRequestDto request) {
        ScheduleGenerationResponseDto response = service.generateSchedule(userId, scheduleRequestId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 3. 후보 스케줄 조회
     */
    @GetMapping("/candidates")
    public ResponseEntity<List<CandidateSchedule>> getCandidateSchedules(@RequestParam String key) {
        List<CandidateSchedule> candidates = service.getCandidateSchedules(key);
        return ResponseEntity.ok(candidates);
    }

    /**
     * 4. 스케줄 확정
     */
    @PostMapping("/requests/{scheduleRequestId}/confirm")
    public ResponseEntity<?> confirmSchedule(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long scheduleRequestId,
            @RequestBody ConfirmScheduleRequestDto request) {
        Schedule finalized = service.finalizeCandidateSchedule(userId, scheduleRequestId, request.getCandidateIndex());
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "근무표 확정 완료",
                "scheduleId", finalized.getId()
        ));
    }

    /**
     * 제출 현황 확인 (미제출 직원 목록)
     */
    @GetMapping("/requests/{storeId}/submission-status")
    public ResponseEntity<Map<String, Object>> checkSubmissionStatus(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long storeId) {
        List<Long> unsubmitted = service.validateAllSubmitted(storeId);
        return ResponseEntity.ok(Map.of(
                "allSubmitted", unsubmitted.isEmpty(),
                "unsubmittedUserIds", unsubmitted
        ));
    }

}
