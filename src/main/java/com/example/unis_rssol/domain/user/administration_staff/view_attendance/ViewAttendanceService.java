package com.example.unis_rssol.domain.user.administration_staff.view_attendance;

import com.example.unis_rssol.domain.schedule.attendance.Attendance;
import com.example.unis_rssol.domain.schedule.attendance.AttendanceRepository;
import com.example.unis_rssol.domain.schedule.generation.entity.WorkShift;
import com.example.unis_rssol.domain.schedule.workshifts.WorkShiftRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ViewAttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final WorkShiftRepository workShiftRepository;

    @Transactional(readOnly = true)
    public ViewAttendanceResponse getEmployeeAttendance(
            Long userStoreId,
            LocalDate startDate,
            LocalDate endDate
    ) {

        List<Attendance> attendanceList =
                attendanceRepository.findByUserStoreIdAndWorkDateBetween(
                        userStoreId, startDate, endDate
                );

        List<WorkShift> shiftList =
                workShiftRepository.findMyShifts(
                        userStoreId,
                        startDate.atStartOfDay(),
                        endDate.atTime(23, 59, 59)
                );

        Map<LocalDate, Attendance> attendanceMap = new HashMap<>();
        for (Attendance attendance : attendanceList) {
            attendanceMap.put(attendance.getWorkDate(), attendance);
        }

        Map<LocalDate, WorkShift> shiftMap = new HashMap<>();
        for (WorkShift shift : shiftList) {
            shiftMap.put(shift.getStartDatetime().toLocalDate(), shift);
        }

        List<ViewAttendanceDayDto> result = new ArrayList<>();

        for (LocalDate date = startDate;
             !date.isAfter(endDate);
             date = date.plusDays(1)) {

            WorkShift shift = shiftMap.get(date);
            Attendance attendance = attendanceMap.get(date);

            String status = resolveAttendance(shift, attendance);

            result.add(new ViewAttendanceDayDto(date, status));
        }

        return new ViewAttendanceResponse(
                userStoreId,
                startDate,
                endDate,
                result
        );
    }

    private String resolveAttendance(WorkShift shift, Attendance attendance) {

        // 1. 휴무
        if (shift == null) {
            return "OFF";
        }

        // 2️. 결근
        if (attendance == null || !attendance.isCheckedIn()) {
            return "ABSENT";
        }

        // 3. 지각
        if (attendance.getCheckInTime()
                .isAfter(shift.getStartDatetime())) {
            return "LATE";
        }

        // 4. 정상 출근
        return "NORMAL";
    }
}
