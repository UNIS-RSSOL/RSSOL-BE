package com.example.unis_rssol.domain.user.administration_staff.view_attendance;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/administration-staff/employees")
public class ViewAttendanceController {

    private final ViewAttendanceService viewAttendanceService;

    @GetMapping("/{userStoreId}/attendance")
    public ViewAttendanceResponse getEmployeeAttendance(
            @PathVariable Long userStoreId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate
    ) {
        return viewAttendanceService.getEmployeeAttendance(
                userStoreId,
                startDate,
                endDate
        );
    }
}
