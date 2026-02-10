package com.example.unis_rssol.domain.schedule.attendance;

import com.example.unis_rssol.domain.schedule.attendance.dto.*;
import com.example.unis_rssol.domain.schedule.generation.entity.WorkShift;
import com.example.unis_rssol.domain.schedule.workshifts.WorkShiftRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final WorkShiftRepository workShiftRepository;

    /** 오늘 근무 상태 조회 (읽기 전용) **/
    @Transactional(readOnly = true)
    public AttendanceTodayResponse getTodayAttendance(Long userStoreId) {
        LocalDate today = LocalDate.now();

        return attendanceRepository
                .findByUserStoreIdAndWorkDate(userStoreId, today)
                .map(this::mapToTodayResponse)
                .orElseGet(() -> createTodayAttendanceIfNeededTransactional(userStoreId, today));
    }

    /** 실제 INSERT는 쓰기 트랜잭션에서 수행 **/
    @Transactional
    public AttendanceTodayResponse createTodayAttendanceIfNeededTransactional(Long userStoreId, LocalDate today) {
        return createTodayAttendanceIfNeeded(userStoreId, today);
    }

    /** 출근 처리 **/
    @Transactional
    public AttendanceCheckInResponse checkIn(Long userStoreId) {
        Attendance attendance = getTodayAttendanceEntity(userStoreId);

        if (attendance.getStatus() == AttendanceStatus.NO_SCHEDULE) {
            throw new IllegalStateException("NO_SCHEDULE");
        }
        if (attendance.isCheckedIn()) {
            throw new IllegalStateException("ALREADY_CHECKED_IN");
        }
        if (attendance.getStatus() == AttendanceStatus.FINISHED) {
            throw new IllegalStateException("ALREADY_FINISHED");
        }

        attendance.checkIn(LocalDateTime.now());

        WorkShift shift = getWorkShiftIfExists(attendance.getWorkShiftId());

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

        if (attendance.getStatus() == AttendanceStatus.NO_SCHEDULE) {
            throw new IllegalStateException("NO_SCHEDULE");
        }
        if (!attendance.isCheckedIn()) {
            throw new IllegalStateException("NOT_CHECKED_IN");
        }
        if (attendance.isCheckedOut()) {
            throw new IllegalStateException("ALREADY_CHECKED_OUT");
        }

        attendance.checkOut(LocalDateTime.now());

        WorkShift shift = getWorkShiftIfExists(attendance.getWorkShiftId());

        return new AttendanceCheckOutResponse(
                "퇴근 처리 완료",
                attendance.getWorkDate(),
                attendance.getStatus(),
                attendance.getCheckOutTime(),
                shift != null ? shift.getStartDatetime() : null,
                shift != null ? shift.getEndDatetime() : null
        );
    }

    /** 하루 1건 Attendance 생성 및 TodayResponse 반환 **/
    private AttendanceTodayResponse createTodayAttendanceIfNeeded(Long userStoreId, LocalDate today) {
        WorkShift todayShift = findTodayWorkShift(userStoreId, today);

        Attendance attendance;
        if (todayShift == null) {
            attendance = Attendance.builder()
                    .userStoreId(userStoreId)
                    .workDate(today)
                    .status(AttendanceStatus.NO_SCHEDULE)
                    .isCheckedIn(false)
                    .isCheckedOut(false)
                    .build();
        } else {
            attendance = Attendance.builder()
                    .userStoreId(userStoreId)
                    .workDate(today)
                    .workShiftId(todayShift.getId())
                    .status(AttendanceStatus.BEFORE_WORK)
                    .isCheckedIn(false)
                    .isCheckedOut(false)
                    .build();
        }

        attendanceRepository.save(attendance);

        return mapToTodayResponse(attendance);
    }

    private AttendanceTodayResponse mapToTodayResponse(Attendance attendance) {
        WorkShift shift = getWorkShiftIfExists(attendance.getWorkShiftId());
        LocalDateTime workStart = shift != null ? shift.getStartDatetime() : null;
        LocalDateTime workEnd = shift != null ? shift.getEndDatetime() : null;

        return new AttendanceTodayResponse(
                attendance.getWorkDate(),
                attendance.getStatus(),
                attendance.isCheckedIn(),
                attendance.isCheckedOut(),
                attendance.getCheckInTime(),
                attendance.getCheckOutTime(),
                workStart,
                workEnd
        );
    }

    /** 오늘 attendance 조회 **/
    @Transactional(readOnly = true)
    private Attendance getTodayAttendanceEntity(Long userStoreId) {
        LocalDate today = LocalDate.now();

        return attendanceRepository
                .findByUserStoreIdAndWorkDate(userStoreId, today)
                .orElseThrow(() -> new IllegalStateException("ATTENDANCE_NOT_FOUND"));
    }

    /** WorkShift 조회 **/
    @Transactional(readOnly = true)
    private WorkShift findTodayWorkShift(Long userStoreId, LocalDate today) {
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.atTime(23, 59, 59);

        List<WorkShift> shifts = workShiftRepository.findMyShifts(userStoreId, start, end);

        return shifts.isEmpty() ? null : shifts.get(0);
    }

    /** WorkShift Id가 있으면 조회 **/
    @Transactional(readOnly = true)
    private WorkShift getWorkShiftIfExists(Long workShiftId) {
        if (workShiftId == null) return null;
        return workShiftRepository.findById(workShiftId).orElse(null);
    }
}
