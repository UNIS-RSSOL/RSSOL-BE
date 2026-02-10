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
        Long workShiftId
) {
    public static AttendanceTodayResponse from(Attendance attendance) {
        return new AttendanceTodayResponse(
                attendance.getWorkDate(),
                attendance.getStatus(),
                attendance.isCheckedIn(),
                attendance.isCheckedOut(),
                attendance.getCheckInTime(),
                attendance.getCheckOutTime(),
                attendance.getWorkShiftId()
        );
    }
}
