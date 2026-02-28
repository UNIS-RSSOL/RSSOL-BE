package com.example.unis_rssol.domain.schedule.attendance.dto;

import com.example.unis_rssol.domain.schedule.attendance.AttendanceStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AttendanceCheckOutResponse(
        String message,
        LocalDate workDate,
        AttendanceStatus status,
        LocalDateTime checkOutTime,
        LocalDateTime workStartTime,
        LocalDateTime workEndTime
) {
}