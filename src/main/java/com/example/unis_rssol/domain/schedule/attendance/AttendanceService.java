package com.example.unis_rssol.domain.schedule.attendance;

import com.example.unis_rssol.domain.schedule.attendance.dto.*;
import com.example.unis_rssol.domain.schedule.generation.entity.WorkShift;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final AttendanceHelper attendanceHelper;

    @Transactional(readOnly = true)
    public AttendanceTodayResponse getTodayAttendance(Long userStoreId) {
        LocalDate today = LocalDate.now();

        return attendanceRepository.findByUserStoreIdAndWorkDate(userStoreId, today)
                .map(attendance -> {
                    WorkShift shift = attendanceHelper.getWorkShiftIfExists(attendance.getWorkShiftId());
                    return attendanceHelper.mapToTodayResponse(attendance, shift);
                })
                .orElseGet(() -> attendanceHelper.createAttendance(userStoreId, today));
    }

    @Transactional
    public AttendanceCheckInResponse checkIn(Long userStoreId) {
        Attendance attendance = getTodayAttendanceEntity(userStoreId);

        if (attendance.getStatus() == AttendanceStatus.NO_SCHEDULE)
            throw new IllegalStateException("오늘 날짜에 해당하는 스케줄이 없습니다.");
        if (attendance.isCheckedIn())
            throw new IllegalStateException("이미 출근 처리가 되었습니다.");
        if (attendance.getStatus() == AttendanceStatus.FINISHED)
            throw new IllegalStateException("이미 퇴근 처리까지 되었습니다.");

        attendance.checkIn(LocalDateTime.now());
        WorkShift shift = attendanceHelper.getWorkShiftIfExists(attendance.getWorkShiftId());

        return new AttendanceCheckInResponse(
                "출근 처리 완료",
                attendance.getWorkDate(),
                attendance.getStatus(),
                attendance.getCheckInTime(),
                shift != null ? shift.getStartDatetime() : null,
                shift != null ? shift.getEndDatetime() : null
        );
    }

    @Transactional
    public AttendanceCheckOutResponse checkOut(Long userStoreId) {
        Attendance attendance = getTodayAttendanceEntity(userStoreId);

        if (attendance.getStatus() == AttendanceStatus.NO_SCHEDULE)
            throw new IllegalStateException("오늘 날짜에 해당하는 스케줄이 없습니다.");
        if (!attendance.isCheckedIn())
            throw new IllegalStateException("출근 처리가 되어 있지 않습니다.");
        if (attendance.isCheckedOut())
            throw new IllegalStateException("이미 퇴근 처리가 되었습니다.");

        attendance.checkOut(LocalDateTime.now());
        WorkShift shift = attendanceHelper.getWorkShiftIfExists(attendance.getWorkShiftId());

        return new AttendanceCheckOutResponse(
                "퇴근 처리 완료",
                attendance.getWorkDate(),
                attendance.getStatus(),
                attendance.getCheckOutTime(),
                shift != null ? shift.getStartDatetime() : null,
                shift != null ? shift.getEndDatetime() : null
        );
    }

    @Transactional(readOnly = true)
    private Attendance getTodayAttendanceEntity(Long userStoreId) {
        LocalDate today = LocalDate.now();
        return attendanceRepository.findByUserStoreIdAndWorkDate(userStoreId, today)
                .orElseThrow(() -> new IllegalStateException("ATTENDANCE_NOT_FOUND"));
    }
}
