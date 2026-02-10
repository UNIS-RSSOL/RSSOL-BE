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
@Transactional
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final WorkShiftRepository workShiftRepository;

    @Transactional(readOnly = true)
    public AttendanceTodayResponse getTodayAttendance(Long userStoreId) {
        LocalDate today = LocalDate.now();

        return attendanceRepository
                .findByUserStoreIdAndWorkDate(userStoreId, today)
                .map(AttendanceTodayResponse::from)
                .orElseGet(() -> createTodayAttendanceIfNeeded(userStoreId, today));
    }

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

        return new AttendanceCheckInResponse(
                "출근 처리 완료",
                attendance.getWorkDate(),
                attendance.getStatus(),
                attendance.getCheckInTime(),
                attendance.getWorkShiftId()
        );
    }

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

        return new AttendanceCheckOutResponse(
                "퇴근 처리 완료",
                attendance.getWorkDate(),
                attendance.getStatus(),
                attendance.getCheckOutTime(),
                attendance.getWorkShiftId()
        );
    }

    // db에 하루 1건 attendance 원칙을 지키기 위함 - 휴무, 근무 전일때 attendance 생성
    private AttendanceTodayResponse createTodayAttendanceIfNeeded(
            Long userStoreId,
            LocalDate today
    ) {
        WorkShift todayShift = findTodayWorkShift(userStoreId, today);

        if (todayShift == null) {
            Attendance attendance = Attendance.builder()
                    .userStoreId(userStoreId)
                    .workDate(today)
                    .status(AttendanceStatus.NO_SCHEDULE)
                    .isCheckedIn(false)
                    .isCheckedOut(false)
                    .build();

            attendanceRepository.save(attendance);
            return AttendanceTodayResponse.from(attendance);
        }

        Attendance attendance = Attendance.builder()
                .userStoreId(userStoreId)
                .workDate(today)
                .workShiftId(todayShift.getId())
                .status(AttendanceStatus.BEFORE_WORK)
                .isCheckedIn(false)
                .isCheckedOut(false)
                .build();

        attendanceRepository.save(attendance);
        return AttendanceTodayResponse.from(attendance);
    }

    // 미리 만들어진 attendance가 없는 상태로 출퇴근 눌렀을 때 예외 처리
    private Attendance getTodayAttendanceEntity(Long userStoreId) {
        LocalDate today = LocalDate.now();

        return attendanceRepository
                .findByUserStoreIdAndWorkDate(userStoreId, today)
                .orElseThrow(() -> new IllegalStateException("ATTENDANCE_NOT_FOUND"));
    }

    private WorkShift findTodayWorkShift(Long userStoreId, LocalDate today) {
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.atTime(23, 59, 59);

        List<WorkShift> shifts = workShiftRepository.findMyShifts(
                userStoreId,
                start,
                end
        );

        return shifts.isEmpty() ? null : shifts.get(0);
    }
}
