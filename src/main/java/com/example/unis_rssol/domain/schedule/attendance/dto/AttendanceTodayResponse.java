package com.example.unis_rssol.domain.schedule.attendance.dto;

import com.example.unis_rssol.domain.schedule.attendance.Attendance;
import com.example.unis_rssol.domain.schedule.attendance.AttendanceStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AttendanceTodayResponse(
        LocalDate workDate,
        AttendanceStatus status,
        boolean isCheckedIn,
        boolean isCheckedOut,
        LocalDateTime checkInTime,
        LocalDateTime checkOutTime,
        LocalDateTime workStartTime,   // 추가
        LocalDateTime workEndTime      // 추가
) {
    public static AttendanceTodayResponse from(Attendance attendance, LocalDateTime start, LocalDateTime end) {
        return new AttendanceTodayResponse(
                attendance.getWorkDate(),
                attendance.getStatus(),
                attendance.isCheckedIn(),
                attendance.isCheckedOut(),
                attendance.getCheckInTime(),
                attendance.getCheckOutTime(),
                start,
                end
        );
    }
}
