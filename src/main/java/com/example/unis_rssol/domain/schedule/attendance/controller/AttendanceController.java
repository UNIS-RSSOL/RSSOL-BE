package com.example.unis_rssol.domain.schedule.attendance.controller;

import com.example.unis_rssol.domain.schedule.attendance.AttendanceService;
import com.example.unis_rssol.domain.schedule.attendance.dto.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    /** 오늘 근무 상태 조회 **/
    @GetMapping("/today")
    public ResponseEntity<AttendanceTodayResponse> getToday(@AuthenticationPrincipal Long userStoreId) {
        AttendanceTodayResponse response = attendanceService.getTodayAttendance(userStoreId);
        return ResponseEntity.ok(response);
    }

    /** 출근 처리 **/
    @PostMapping("/check-in")
    public ResponseEntity<AttendanceCheckInResponse> checkIn(@AuthenticationPrincipal Long userStoreId) {
        AttendanceCheckInResponse response = attendanceService.checkIn(userStoreId);
        return ResponseEntity.ok(response);
    }

    /** 퇴근 처리 **/
    @PostMapping("/check-out")
    public ResponseEntity<AttendanceCheckOutResponse> checkOut(@AuthenticationPrincipal Long userStoreId) {
        AttendanceCheckOutResponse response = attendanceService.checkOut(userStoreId);
        return ResponseEntity.ok(response);
    }
}
