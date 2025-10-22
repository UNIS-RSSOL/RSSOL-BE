package com.example.unis_rssol.schedule.generation;

import com.example.unis_rssol.global.exception.NotFoundException;
import com.example.unis_rssol.schedule.entity.Schedule;
import com.example.unis_rssol.schedule.generation.dto.CandidateSchedule;
import com.example.unis_rssol.schedule.generation.dto.ConfirmScheduleRequestDto;
import com.example.unis_rssol.schedule.generation.dto.ScheduleGenerationRequestDto;
import com.example.unis_rssol.schedule.generation.dto.ScheduleGenerationResponseDto;
import com.fasterxml.jackson.core.JsonProcessingException;
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
public class ScheduleGenerationController {
    private final ScheduleGenerationService service;


    public ScheduleGenerationController(ScheduleGenerationService service) {
        this.service = service;
    }     //주입받아서 컨트롤러 생성한다.

    @PostMapping("/generate")
    public ResponseEntity<ScheduleGenerationResponseDto> createSchedule(@AuthenticationPrincipal Long userId, @RequestBody ScheduleGenerationRequestDto request) {
        ScheduleGenerationResponseDto response = service.createSchedules(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/candidates/{key}")
    public ResponseEntity<List<CandidateSchedule>> getCandidateSchedules(@PathVariable("key") String key) {
        List<CandidateSchedule> candidates = service.getCandidateSchedules(key);
        return ResponseEntity.ok(candidates);
    }

    @PostMapping("/confirm")
    public ResponseEntity<?> confirmSchedule(@AuthenticationPrincipal Long userId, @RequestBody ConfirmScheduleRequestDto request) {
        // Redis에서 후보 불러오고 startDate/endDate는 CandidateSchedule 생성 시 기록되어 있어야 함
        Schedule finalized = service.finalizeCandidateSchedule(userId, request.getCandidateKey(), request.getStartDate(), request.getEndDate());
        if (finalized == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("status", "error", "message", "Failed to confirm schedule"));
        }
        return ResponseEntity.ok(Map.of("status", "success", "message", "근무표 확정 완료", "scheduleId", finalized.getId()));
    }


    @GetMapping("/redis")
    public String redisTest() throws JsonProcessingException {
        List<CandidateSchedule> dummy = List.of(new CandidateSchedule(1L));
        service.testRedis(dummy);
        return "Redis 테스트 완료";
    }
}
