package com.example.unis_rssol.schedule.workshifts;

import com.example.unis_rssol.global.auth.annotation.OwnerOnly;
import com.example.unis_rssol.schedule.entity.WorkShift;
import com.example.unis_rssol.schedule.repository.WorkShiftRepository;
import com.example.unis_rssol.schedule.workshifts.dto.MyWorkShiftDto;
import com.example.unis_rssol.schedule.workshifts.dto.WorkShiftCreateDto;
import com.example.unis_rssol.schedule.workshifts.dto.WorkShiftDto;
import com.example.unis_rssol.schedule.workshifts.dto.WorkShiftUpdateDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@RestController
@Slf4j
@RequestMapping("/api/schedules")
public class WorkShiftController {
    private final WorkShiftService service;
    private final WorkShiftRepository workShiftRepository;

    public WorkShiftController(WorkShiftService service, WorkShiftRepository workShiftRepository) {
        this.service = service;
        this.workShiftRepository = workShiftRepository;
    }

    @GetMapping("")
    public ResponseEntity<Map<String, Object>> getWorkShifts(@AuthenticationPrincipal Long userId) {
        List<WorkShiftDto> workShifts = service.getWorkShifts(userId);

        Map<String, Object> response = new HashMap<>();
        response.put("work_shifts", workShifts);

        return ResponseEntity.ok(response);
    }

    /** 조회 **/
    @GetMapping("/store/week")
    public ResponseEntity<List<WorkShiftDto>> getStoreWorkShiftsByWeek(
            @AuthenticationPrincipal Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        List<WorkShift> workShifts = service.getWorkShiftsByPeriod(userId, start, end);
        List<WorkShiftDto> dtos = workShifts.stream().map(WorkShiftDto::new).toList();
        return ResponseEntity.ok(dtos);
    }

    /** 내 스케쥴 조회 **/
    @GetMapping("/me/week")
    public ResponseEntity<List<MyWorkShiftDto>> getMyWorkShiftsByWeek(
            @AuthenticationPrincipal Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        List<WorkShift> workShifts = service.getMyWorkShiftsByPeriod(userId, start, end);
        List<MyWorkShiftDto> dtos = workShifts.stream().map(MyWorkShiftDto::new).toList();
        return ResponseEntity.ok(dtos);
    }

    /** 생성 **/
    @OwnerOnly
    @PostMapping("/workshifts")
    public ResponseEntity<WorkShiftDto> createWorkShift(
            @AuthenticationPrincipal Long userId,
            @RequestBody WorkShiftCreateDto dto) {
        WorkShiftDto result = service.createWorkShift(userId, dto);
        return ResponseEntity.ok(result);
    }

    /** 수정 **/
    @OwnerOnly
    @PatchMapping("/workshifts/{shiftId}")
    public ResponseEntity<WorkShiftDto> updateWorkShift(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long shiftId,
            @RequestBody WorkShiftUpdateDto dto) {
        WorkShiftDto result = service.updateWorkShift(userId, shiftId, dto);
        return ResponseEntity.ok(result);
    }

    /** 삭제 **/
    @OwnerOnly
    @DeleteMapping("/workshifts/{shiftId}")
    public ResponseEntity<Void> deleteWorkShift(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long shiftId) {
        service.deleteWorkShift(userId, shiftId);
        return ResponseEntity.noContent().build();
    }
}
