package com.example.unis_rssol.domain.user.administration_staff.view_attendance;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@AllArgsConstructor
public class ViewAttendanceResponse {

    private Long userStoreId;

    private String staffName;
    private String role;
    private int totalAttendance;
    private int totalLateCount;
    private int totalAbsentCount;

    private LocalDate startDate;
    private LocalDate endDate;

    private List<ViewAttendanceDayDto> attendances;
}