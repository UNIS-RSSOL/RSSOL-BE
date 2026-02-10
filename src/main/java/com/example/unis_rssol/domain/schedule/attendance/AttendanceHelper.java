package com.example.unis_rssol.domain.schedule.attendance;

import com.example.unis_rssol.domain.schedule.attendance.dto.AttendanceTodayResponse;
import com.example.unis_rssol.domain.schedule.generation.entity.WorkShift;
import com.example.unis_rssol.domain.schedule.workshifts.WorkShiftRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AttendanceHelper {

    private final AttendanceRepository attendanceRepository;
    private final WorkShiftRepository workShiftRepository;

    /** 오늘 Attendance 생성 (REQUIRES_NEW) **/
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AttendanceTodayResponse createAttendance(Long userStoreId, LocalDate today) {
        log.info("[AttendanceHelper.createAttendance] START userStoreId={}, today={}", userStoreId, today);

        // 오늘 근무 스케줄 조회 (겹치는 스케줄 포함)
        WorkShift todayShift = findTodayWorkShift(userStoreId, today);

        log.info("[AttendanceHelper.createAttendance] todayShift: {}", todayShift);

        Attendance attendance;
        if (todayShift != null) { // 오늘 스케줄 존재
            attendance = Attendance.builder()
                    .userStoreId(userStoreId)
                    .workDate(today)
                    .workShiftId(todayShift.getId())
                    .status(AttendanceStatus.BEFORE_WORK)
                    .isCheckedIn(false)
                    .isCheckedOut(false)
                    .build();
            log.info("[AttendanceHelper.createAttendance] Attendance will be BEFORE_WORK");
        } else { // 스케줄 없으면 NO_SCHEDULE
            attendance = Attendance.builder()
                    .userStoreId(userStoreId)
                    .workDate(today)
                    .status(AttendanceStatus.NO_SCHEDULE)
                    .isCheckedIn(false)
                    .isCheckedOut(false)
                    .build();
            log.info("[AttendanceHelper.createAttendance] Attendance will be NO_SCHEDULE");
        }

        attendanceRepository.save(attendance);
        log.info("[AttendanceHelper.createAttendance] Attendance saved: {}", attendance);

        return mapToTodayResponse(attendance, todayShift);
    }

    /** Attendance → DTO 변환 **/
    public AttendanceTodayResponse mapToTodayResponse(Attendance attendance, WorkShift shift) {
        AttendanceStatus status = attendance.getStatus();

        // 오늘 스케줄 존재 시 NO_SCHEDULE 상태면 BEFORE_WORK로 교정
        if (shift != null && status == AttendanceStatus.NO_SCHEDULE) {
            log.info("[AttendanceHelper.mapToTodayResponse] Correcting status from NO_SCHEDULE -> BEFORE_WORK");
            status = AttendanceStatus.BEFORE_WORK;
        }

        AttendanceTodayResponse response = new AttendanceTodayResponse(
                attendance.getWorkDate(),
                status,
                attendance.isCheckedIn(),
                attendance.isCheckedOut(),
                attendance.getCheckInTime(),
                attendance.getCheckOutTime(),
                shift != null ? shift.getStartDatetime() : null,
                shift != null ? shift.getEndDatetime() : null
        );

        log.info("[AttendanceHelper.mapToTodayResponse] DTO response: {}", response);
        return response;
    }

    /** 오늘 범위 WorkShift 조회 (겹치는 스케줄 포함) **/
    private WorkShift findTodayWorkShift(Long userStoreId, LocalDate today) {
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.atTime(23, 59, 59);

        List<WorkShift> shifts = workShiftRepository.findShiftsOverlappingToday(userStoreId, start, end);
        log.info("[AttendanceHelper.findTodayWorkShift] Shifts found for userStoreId={}: {}", userStoreId, shifts);

        if (shifts.isEmpty()) {
            log.info("[AttendanceHelper.findTodayWorkShift] No WorkShift found today");
            return null;
        }

        WorkShift shift = shifts.get(0);
        log.info("[AttendanceHelper.findTodayWorkShift] Using first WorkShift: {}", shift);
        return shift;
    }

    /** WorkShift 단건 조회 **/
    public WorkShift getWorkShiftIfExists(Long workShiftId) {
        if (workShiftId == null) return null;
        WorkShift shift = workShiftRepository.findById(workShiftId).orElse(null);
        log.info("[AttendanceHelper.getWorkShiftIfExists] workShiftId={}, shift={}", workShiftId, shift);
        return shift;
    }
}
