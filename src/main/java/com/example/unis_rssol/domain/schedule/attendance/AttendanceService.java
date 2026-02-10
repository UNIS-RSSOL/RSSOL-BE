package com.example.unis_rssol.domain.schedule.attendance;

import com.example.unis_rssol.domain.schedule.attendance.dto.*;
import com.example.unis_rssol.domain.schedule.generation.entity.WorkShift;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final AttendanceHelper attendanceHelper;

    /** 오늘 근무 상태 조회 **/
    @Transactional(readOnly = true)
    public AttendanceTodayResponse getTodayAttendance(Long userStoreId) {
        LocalDate today = LocalDate.now();
        log.info("[AttendanceService.getTodayAttendance] START userStoreId={}, today={}", userStoreId, today);

        return attendanceRepository.findByUserStoreIdAndWorkDate(userStoreId, today)
                .map(attendance -> {
                    WorkShift shift = attendanceHelper.getWorkShiftIfExists(attendance.getWorkShiftId());
                    log.info("[AttendanceService.getTodayAttendance] Found attendance: {}, shift: {}", attendance, shift);
                    return attendanceHelper.mapToTodayResponse(attendance, shift);
                })
                .orElseGet(() -> {
                    log.info("[AttendanceService.getTodayAttendance] No attendance found, creating new one");
                    return attendanceHelper.createAttendance(userStoreId, today);
                });
    }

    /** 출근 처리 **/
    @Transactional
    public AttendanceCheckInResponse checkIn(Long userStoreId) {
        log.info("[AttendanceService.checkIn] START userStoreId={}", userStoreId);
        Attendance attendance = getTodayAttendanceEntity(userStoreId);

        if (attendance.getStatus() == AttendanceStatus.NO_SCHEDULE)
            throw new IllegalStateException("NO_SCHEDULE");
        if (attendance.isCheckedIn())
            throw new IllegalStateException("ALREADY_CHECKED_IN");
        if (attendance.getStatus() == AttendanceStatus.FINISHED)
            throw new IllegalStateException("ALREADY_FINISHED");

        attendance.checkIn(LocalDateTime.now());
        WorkShift shift = attendanceHelper.getWorkShiftIfExists(attendance.getWorkShiftId());

        log.info("[AttendanceService.checkIn] Checked in attendance: {}, shift: {}", attendance, shift);

        return new AttendanceCheckInResponse(
                "출근 처리 완료",
                attendance.getWorkDate(),
                attendance.getStatus(),
                attendance.getCheckInTime(),
                shift != null ? shift.getStartDatetime() : null,
                shift != null ? shift.getEndDatetime() : null
        );
    }

    /** 퇴근 처리 **/
    @Transactional
    public AttendanceCheckOutResponse checkOut(Long userStoreId) {
        Attendance attendance = getTodayAttendanceEntity(userStoreId);

        if (attendance.getStatus() == AttendanceStatus.NO_SCHEDULE)
            throw new IllegalStateException("NO_SCHEDULE");
        if (!attendance.isCheckedIn())
            throw new IllegalStateException("NOT_CHECKED_IN");
        if (attendance.isCheckedOut())
            throw new IllegalStateException("ALREADY_CHECKED_OUT");

        attendance.checkOut(LocalDateTime.now());
        WorkShift shift = attendanceHelper.getWorkShiftIfExists(attendance.getWorkShiftId());

        log.info("[AttendanceService.checkOut] Checked out attendance: {}, shift: {}", attendance, shift);

        return new AttendanceCheckOutResponse(
                "퇴근 처리 완료",
                attendance.getWorkDate(),
                attendance.getStatus(),
                attendance.getCheckOutTime(),
                shift != null ? shift.getStartDatetime() : null,
                shift != null ? shift.getEndDatetime() : null
        );
    }

    /** 오늘 Attendance 조회 **/
    @Transactional(readOnly = true)
    private Attendance getTodayAttendanceEntity(Long userStoreId) {
        LocalDate today = LocalDate.now();
        Attendance attendance = attendanceRepository.findByUserStoreIdAndWorkDate(userStoreId, today)
                .orElseThrow(() -> new IllegalStateException("ATTENDANCE_NOT_FOUND"));
        log.info("[AttendanceService.getTodayAttendanceEntity] Attendance found: {}", attendance);
        return attendance;
    }
}
