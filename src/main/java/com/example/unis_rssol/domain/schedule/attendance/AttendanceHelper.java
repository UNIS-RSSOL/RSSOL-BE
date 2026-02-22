package com.example.unis_rssol.domain.schedule.attendance;

import com.example.unis_rssol.domain.schedule.attendance.dto.AttendanceTodayResponse;
import com.example.unis_rssol.domain.schedule.generation.entity.WorkShift;
import com.example.unis_rssol.domain.schedule.workshifts.WorkShiftRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AttendanceHelper {

    private final AttendanceRepository attendanceRepository;
    private final WorkShiftRepository workShiftRepository;

    @Transactional
    public AttendanceTodayResponse createAttendance(Long userStoreId, LocalDate today) {

        WorkShift todayShift = findTodayWorkShift(userStoreId, today);

        Attendance attendance;

        if (todayShift != null) {
            attendance = Attendance.builder()
                    .userStoreId(userStoreId)
                    .workDate(today)
                    .workShiftId(todayShift.getId())
                    .status(AttendanceStatus.BEFORE_WORK)
                    .isCheckedIn(false)
                    .isCheckedOut(false)
                    .build();
        } else {
            attendance = Attendance.builder()
                    .userStoreId(userStoreId)
                    .workDate(today)
                    .status(AttendanceStatus.NO_SCHEDULE)
                    .isCheckedIn(false)
                    .isCheckedOut(false)
                    .build();
        }

        attendanceRepository.save(attendance);

        return mapToTodayResponse(attendance, todayShift);
    }


    public AttendanceTodayResponse mapToTodayResponse(Attendance attendance, WorkShift shift) {

        return new AttendanceTodayResponse(
                attendance.getWorkDate(),
                attendance.getStatus(),
                attendance.isCheckedIn(),
                attendance.isCheckedOut(),
                attendance.getCheckInTime(),
                attendance.getCheckOutTime(),
                shift != null ? shift.getStartDatetime() : null,
                shift != null ? shift.getEndDatetime() : null
        );
    }

    private WorkShift findTodayWorkShift(Long userStoreId, LocalDate today) {

        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.atTime(23, 59, 59);

        List<WorkShift> shifts =
                workShiftRepository.findShiftsOverlappingToday(
                        userStoreId, start, end
                );

        return shifts.isEmpty() ? null : shifts.get(0);
    }

    public WorkShift getWorkShiftIfExists(Long workShiftId) {
        if (workShiftId == null) return null;
        return workShiftRepository.findById(workShiftId).orElse(null);
    }
}