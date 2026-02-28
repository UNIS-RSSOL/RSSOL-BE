package com.example.unis_rssol.domain.user.administration_staff.view_attendance;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class ViewAttendanceDayDto {
    private LocalDate workDate;
    private String attendanceStatus; // OFF, ABSENT, LATE, NORMAL
}
