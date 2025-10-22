package com.example.unis_rssol.schedule.generation;

import com.example.unis_rssol.schedule.generation.dto.CandidateSchedule;
import com.example.unis_rssol.schedule.generation.dto.ScheduleGenerationRequestDto;
import com.example.unis_rssol.schedule.generation.dto.ScheduleGenerationResponseDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequestMapping("/api/schedules")
public class ScheduleGenerationController {
    private final ScheduleGenerationService service;


    public ScheduleGenerationController(ScheduleGenerationService service) {this.service = service;}     //주입받아서 컨트롤러 생성한다.

    @PostMapping("/generate")
    public ResponseEntity<ScheduleGenerationResponseDto> createSchedule(@AuthenticationPrincipal Long userId, @RequestBody ScheduleGenerationRequestDto request){
        ScheduleGenerationResponseDto response = service.createSchedules(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/candidates/{key}")
    public ResponseEntity<List<CandidateSchedule>> getCandidateSchedules(@PathVariable("key") String key){
        List<CandidateSchedule> candidates = service.getCandidateSchedules(key);
        return ResponseEntity.ok(candidates);
    }

    @GetMapping("/redis")
    public String redisTest() throws JsonProcessingException {
        List<CandidateSchedule> dummy = List.of(new CandidateSchedule(1L));
        service.testRedis(dummy);
        return "Redis 테스트 완료";
    }
}
