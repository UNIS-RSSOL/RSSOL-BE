package com.example.unis_rssol.domain.schedule.attendance.dto;

import com.example.unis_rssol.domain.schedule.attendance.AttendanceStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AttendanceCheckInResponse(
        String message,
        LocalDate workDate,
        AttendanceStatus status,
        LocalDateTime checkInTime,
        LocalDateTime workStartTime,
        LocalDateTime workEndTime
) {
}
