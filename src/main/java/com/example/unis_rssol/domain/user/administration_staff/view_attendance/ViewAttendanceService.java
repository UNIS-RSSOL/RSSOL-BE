package com.example.unis_rssol.domain.user.administration_staff.view_attendance;

import com.example.unis_rssol.domain.schedule.attendance.Attendance;
import com.example.unis_rssol.domain.schedule.attendance.AttendanceRepository;
import com.example.unis_rssol.domain.schedule.generation.entity.WorkShift;
import com.example.unis_rssol.domain.schedule.workshifts.WorkShiftRepository;
import com.example.unis_rssol.domain.store.UserStore;
import com.example.unis_rssol.domain.store.UserStoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ViewAttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final WorkShiftRepository workShiftRepository;
    private final UserStoreRepository userStoreRepository;

    @Transactional(readOnly = true)
    public ViewAttendanceResponse getEmployeeAttendance(
            Long ownerId,
            Long userStoreId,
            LocalDate startDate,
            LocalDate endDate
    ) {

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("INVALID_DATE_RANGE");
        }

        UserStore userStore = userStoreRepository.findById(userStoreId)
                .orElseThrow(() -> new IllegalArgumentException("USER_STORE_NOT_FOUND"));

        boolean isOwner = userStoreRepository
                .findByUser_IdAndStore_Id(ownerId, userStore.getStore().getId())
                .stream()
                .anyMatch(us -> us.getPosition() == UserStore.Position.OWNER);

        if (!isOwner) {
            throw new IllegalArgumentException("ACCESS_DENIED");
        }

        String staffName = userStore.getUser().getUsername();
        String role = userStore.getPosition().name();

        List<Attendance> attendanceList =
                attendanceRepository.findByUserStoreIdAndWorkDateBetween(
                        userStoreId, startDate, endDate
                );

        List<WorkShift> shiftList =
                workShiftRepository.findMyShifts(
                        userStore.getUser().getId(),
                        startDate.atStartOfDay(),
                        endDate.atTime(23, 59, 59)
                );

        Map<LocalDate, Attendance> attendanceMap = new HashMap<>();
        for (Attendance a : attendanceList) {
            attendanceMap.put(a.getWorkDate(), a);
        }

        Map<LocalDate, WorkShift> shiftMap = new HashMap<>();
        for (WorkShift s : shiftList) {
            shiftMap.put(s.getStartDatetime().toLocalDate(), s);
        }

        List<ViewAttendanceDayDto> result = new ArrayList<>();

        int normalCount = 0;
        int lateCount = 0;
        int absentCount = 0;

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {

            WorkShift shift = shiftMap.get(date);
            Attendance attendance = attendanceMap.get(date);

            String status = resolveAttendance(shift, attendance);

            if ("NORMAL".equals(status)) {
                normalCount++;
            }

            if ("LATE".equals(status)) {
                lateCount++;
            }

            if ("ABSENT".equals(status)) {
                absentCount++;
            }

            result.add(new ViewAttendanceDayDto(date, status));
        }

        return new ViewAttendanceResponse(
                userStoreId,
                staffName,
                role,
                normalCount,
                lateCount,
                absentCount,
                startDate,
                endDate,
                result
        );
    }

    private String resolveAttendance(WorkShift shift, Attendance attendance) {

        if (shift == null) {
            return "OFF";
        }

        if (attendance == null || !attendance.isCheckedIn()) {
            return "ABSENT";
        }

        if (attendance.getCheckInTime().isAfter(shift.getStartDatetime())) {
            return "LATE";
        }

        return "NORMAL";
    }
}