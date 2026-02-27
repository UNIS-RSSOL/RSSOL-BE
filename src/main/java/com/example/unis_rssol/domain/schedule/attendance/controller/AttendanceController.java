package com.example.unis_rssol.domain.schedule.attendance.controller;

import com.example.unis_rssol.domain.schedule.attendance.AttendanceService;
import com.example.unis_rssol.domain.schedule.attendance.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @GetMapping("/today")
    public AttendanceTodayResponse getToday(@AuthenticationPrincipal Long userId) {
        return attendanceService.getTodayAttendanceByUserId(userId);
    }

    @PostMapping("/check-in")
    public AttendanceCheckInResponse checkIn(@AuthenticationPrincipal Long userId) {
        return attendanceService.checkInByUserId(userId);
    }

    @PostMapping("/check-out")
    public AttendanceCheckOutResponse checkOut(@AuthenticationPrincipal Long userId) {
        return attendanceService.checkOutByUserId(userId);
    }
}