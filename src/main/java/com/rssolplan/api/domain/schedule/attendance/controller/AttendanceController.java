package com.rssolplan.api.domain.schedule.attendance.controller;

import com.rssolplan.api.domain.schedule.attendance.AttendanceService;
import com.rssolplan.api.domain.schedule.attendance.dto.AttendanceCheckInResponse;
import com.rssolplan.api.domain.schedule.attendance.dto.AttendanceCheckOutResponse;
import com.rssolplan.api.domain.schedule.attendance.dto.AttendanceTodayResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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