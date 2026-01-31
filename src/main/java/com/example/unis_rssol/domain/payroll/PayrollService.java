package com.example.unis_rssol.domain.payroll;

import com.example.unis_rssol.domain.payroll.dto.*;
import com.example.unis_rssol.domain.payroll.util.LaborLawConstants;
import com.example.unis_rssol.domain.payroll.util.TimeRangeUtil;
import com.example.unis_rssol.domain.schedule.generation.entity.WorkShift;
import com.example.unis_rssol.domain.schedule.workshifts.WorkShiftRepository;
import com.example.unis_rssol.domain.store.Store;
import com.example.unis_rssol.domain.store.UserStore;
import com.example.unis_rssol.domain.store.UserStoreRepository;
import com.example.unis_rssol.global.exception.ForbiddenException;
import com.example.unis_rssol.global.exception.NotFoundException;
import com.example.unis_rssol.global.security.AuthorizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * [생성 이유]
 * 급여 조회 로직을 담당하는 Service 계층.
 * PayCalculatorService는 수당 '계산' 로직만 담당하고,
 * 이 서비스는 WorkShift 조회 + 계산 결과 집계를 담당하여 SRP 준수.
 * <p>
 * [역할]
 * - OWNER: 매장 전체 직원 급여 조회
 * - STAFF: 본인이 근무하는 모든 매장의 급여 조회
 * - 주휴수당 계산 포함
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayrollService {

    private final PayCalculatorService payCalculatorService;
    private final WorkShiftRepository workShiftRepository;
    private final UserStoreRepository userStoreRepository;
    private final AuthorizationService authorizationService;

    // 기본 시급 (추후 UserStore 또는 Store에서 관리 가능)
    private static final int DEFAULT_HOURLY_WAGE = LaborLawConstants.MINIMUM_WAGE_2025;
    // 5인 이상 사업장 여부 (추후 Store 설정으로 관리 가능)
    private static final boolean DEFAULT_FIVE_OR_MORE = true;

    /**
     * OWNER: 활성 매장의 전체 직원 급여 조회
     */
    @Transactional(readOnly = true)
    public OwnerPayrollSummaryDto getStorePayrollSummary(Long userId, int year, int month) {
        Long storeId = authorizationService.getActiveStoreIdOrThrow(userId);

        // OWNER 권한 확인
        UserStore ownerUserStore = userStoreRepository.findByUser_IdAndStore_Id(userId, storeId)
                .orElseThrow(() -> new ForbiddenException("해당 매장에 대한 권한이 없습니다."));

        if (ownerUserStore.getPosition() != UserStore.Position.OWNER) {
            throw new ForbiddenException("매장 급여 조회는 OWNER만 가능합니다.");
        }

        Store store = ownerUserStore.getStore();

        // 해당 월의 시작/종료 시간
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDateTime monthStart = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime monthEnd = yearMonth.atEndOfMonth().plusDays(1).atStartOfDay();

        // 매장의 모든 STAFF 조회
        List<UserStore> staffList = userStoreRepository.findByStore_IdAndPosition(storeId, UserStore.Position.STAFF);

        // 해당 월의 모든 WorkShift 조회
        List<WorkShift> allShifts = workShiftRepository.findByStoreIdAndMonthRange(storeId, monthStart, monthEnd);

        // UserStore별로 그룹화
        Map<Long, List<WorkShift>> shiftsByUserStore = allShifts.stream()
                .collect(Collectors.groupingBy(ws -> ws.getUserStore().getId()));

        // 각 직원별 급여 계산
        List<StaffPayrollResponseDto> staffPayrolls = new ArrayList<>();
        BigDecimal totalBasePay = BigDecimal.ZERO;
        BigDecimal totalOvertimePay = BigDecimal.ZERO;
        BigDecimal totalNightPay = BigDecimal.ZERO;
        BigDecimal totalHolidayPay = BigDecimal.ZERO;
        BigDecimal totalWeeklyAllowance = BigDecimal.ZERO;
        BigDecimal grandTotalPay = BigDecimal.ZERO;

        for (UserStore staff : staffList) {
            List<WorkShift> staffShifts = shiftsByUserStore.getOrDefault(staff.getId(), Collections.emptyList());
            StaffPayrollResponseDto staffPayroll = calculateStaffPayroll(staff, staffShifts, year, month);
            staffPayrolls.add(staffPayroll);

            totalBasePay = totalBasePay.add(staffPayroll.getBasePay());
            totalOvertimePay = totalOvertimePay.add(staffPayroll.getOvertimePay());
            totalNightPay = totalNightPay.add(staffPayroll.getNightPay());
            totalHolidayPay = totalHolidayPay.add(staffPayroll.getHolidayPay());
            totalWeeklyAllowance = totalWeeklyAllowance.add(staffPayroll.getWeeklyAllowance());
            grandTotalPay = grandTotalPay.add(staffPayroll.getTotalPay());
        }


        return OwnerPayrollSummaryDto.builder()
                .storeId(storeId)
                .storeName(store.getName())
                .year(year)
                .month(month)
                .totalStaffCount(staffList.size())
                .totalBasePay(totalBasePay)
                .totalOvertimePay(totalOvertimePay)
                .totalNightPay(totalNightPay)
                .totalHolidayPay(totalHolidayPay)
                .totalWeeklyAllowance(totalWeeklyAllowance)
                .grandTotalPay(grandTotalPay)
                .staffPayrolls(staffPayrolls)
                .build();
    }

    /**
     * OWNER: 특정 직원의 급여 상세 조회
     */
    @Transactional(readOnly = true)
    public StaffPayrollResponseDto getStaffPayrollDetail(Long userId, Long userStoreId, int year, int month) {
        Long storeId = authorizationService.getActiveStoreIdOrThrow(userId);

        // OWNER 권한 확인
        UserStore ownerUserStore = userStoreRepository.findByUser_IdAndStore_Id(userId, storeId)
                .orElseThrow(() -> new ForbiddenException("해당 매장에 대한 권한이 없습니다."));

        if (ownerUserStore.getPosition() != UserStore.Position.OWNER) {
            throw new ForbiddenException("직원 급여 조회는 OWNER만 가능합니다.");
        }

        // 대상 직원 조회
        UserStore targetStaff = userStoreRepository.findById(userStoreId)
                .orElseThrow(() -> new NotFoundException("해당 직원을 찾을 수 없습니다."));

        if (!targetStaff.getStore().getId().equals(storeId)) {
            throw new ForbiddenException("해당 직원은 이 매장 소속이 아닙니다.");
        }

        // 해당 월의 시작/종료 시간
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDateTime monthStart = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime monthEnd = yearMonth.atEndOfMonth().plusDays(1).atStartOfDay();

        List<WorkShift> staffShifts = workShiftRepository.findByUserStoreIdAndMonthRange(userStoreId, monthStart, monthEnd);

        return calculateStaffPayroll(targetStaff, staffShifts, year, month);
    }

    /**
     * STAFF: 본인이 근무하는 모든 매장의 급여 조회
     */
    @Transactional(readOnly = true)
    public List<StaffMyPayrollResponseDto> getMyPayrolls(Long userId, int year, int month) {
        // 사용자가 속한 모든 UserStore 조회
        List<UserStore> myUserStores = userStoreRepository.findByUser_Id(userId);

        if (myUserStores.isEmpty()) {
            return Collections.emptyList();
        }

        // 해당 월의 시작/종료 시간
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDateTime monthStart = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime monthEnd = yearMonth.atEndOfMonth().plusDays(1).atStartOfDay();

        List<StaffMyPayrollResponseDto> results = new ArrayList<>();

        for (UserStore userStore : myUserStores) {
            List<WorkShift> shifts = workShiftRepository.findByUserStoreIdAndMonthRange(
                    userStore.getId(), monthStart, monthEnd);

            StaffMyPayrollResponseDto payroll = calculateMyPayroll(userStore, shifts, year, month);
            results.add(payroll);
        }

        return results;
    }

    /**
     * 개별 직원 급여 계산 (OWNER 조회용)
     */
    private StaffPayrollResponseDto calculateStaffPayroll(UserStore staff, List<WorkShift> shifts, int year, int month) {
        int hourlyWage = DEFAULT_HOURLY_WAGE;
        boolean isFiveOrMore = DEFAULT_FIVE_OR_MORE;

        // 일별로 그룹화하여 연장근로 계산
        Map<LocalDate, List<WorkShift>> shiftsByDate = shifts.stream()
                .collect(Collectors.groupingBy(ws -> ws.getStartDatetime().toLocalDate()));

        BigDecimal totalBasePay = BigDecimal.ZERO;
        BigDecimal totalOvertimePay = BigDecimal.ZERO;
        BigDecimal totalNightPay = BigDecimal.ZERO;
        BigDecimal totalHolidayPay = BigDecimal.ZERO;

        long totalWorkMinutes = 0;
        long totalBreakMinutes = 0;
        long totalOvertimeMinutes = 0;
        long totalNightMinutes = 0;
        long totalHolidayMinutes = 0;
        int lateCount = 0;

        for (Map.Entry<LocalDate, List<WorkShift>> entry : shiftsByDate.entrySet()) {
            LocalDate date = entry.getKey();
            List<WorkShift> dayShifts = entry.getValue();

            long dailyWorkedMinutes = 0;
            for (WorkShift shift : dayShifts) {
                // 휴게시간 계산 (4시간 이상 30분, 8시간 이상 1시간)
                long shiftMinutes = TimeRangeUtil.calculateWorkMinutes(
                        shift.getStartDatetime(), shift.getEndDatetime(), 0);
                long breakMinutes = calculateBreakMinutes(shiftMinutes);
                totalBreakMinutes += breakMinutes;

                // 휴일 여부 (일요일을 주휴일로 가정)
                boolean isHoliday = date.getDayOfWeek() == DayOfWeek.SUNDAY;

                WorkTimeDto workTime = WorkTimeDto.builder()
                        .workShiftId(shift.getId())
                        .startTime(shift.getStartDatetime())
                        .endTime(shift.getEndDatetime())
                        .isHoliday(isHoliday)
                        .breakMinutes(breakMinutes)
                        .build();

                PayDetailDto payDetail = payCalculatorService.calculatePay(
                        workTime, hourlyWage, isFiveOrMore, dailyWorkedMinutes);


                totalBasePay = totalBasePay.add(payDetail.getBasePay());
                totalOvertimePay = totalOvertimePay.add(payDetail.getOvertimePay());
                totalNightPay = totalNightPay.add(payDetail.getNightPay());
                totalHolidayPay = totalHolidayPay.add(payDetail.getHolidayPay());

                totalWorkMinutes += payDetail.getTotalWorkMinutes();
                totalOvertimeMinutes += payDetail.getOvertimeMinutes();
                totalNightMinutes += payDetail.getNightWorkMinutes();
                totalHolidayMinutes += payDetail.getHolidayWorkMinutes() + payDetail.getHolidayOvertimeMinutes();

                dailyWorkedMinutes += payDetail.getTotalWorkMinutes();

                // 지각 체크
                if (shift.getShiftStatus() == WorkShift.ShiftStatus.LATE) {
                    lateCount++;
                }
            }
        }

        // 주휴수당 계산
        BigDecimal weeklyAllowance = calculateWeeklyAllowance(shifts, hourlyWage, year, month);

        BigDecimal totalPay = totalBasePay
                .add(totalOvertimePay)
                .add(totalNightPay)
                .add(totalHolidayPay)
                .add(weeklyAllowance);

        return StaffPayrollResponseDto.builder()
                .userStoreId(staff.getId())
                .staffName(staff.getUser().getUsername())
                .hourlyWage(hourlyWage)
                .totalWorkMinutes(totalWorkMinutes)
                .breakMinutes(totalBreakMinutes)
                .overtimeMinutes(totalOvertimeMinutes)
                .nightWorkMinutes(totalNightMinutes)
                .holidayWorkMinutes(totalHolidayMinutes)
                .basePay(totalBasePay)
                .overtimePay(totalOvertimePay)
                .nightPay(totalNightPay)
                .holidayPay(totalHolidayPay)
                .weeklyAllowance(weeklyAllowance)
                .totalPay(totalPay)
                .totalShiftCount(shifts.size())
                .lateCount(lateCount)
                .absenceCount(0) // 결근은 별도 로직 필요
                .build();
    }

    /**
     * 본인 급여 계산 (STAFF 조회용)
     */
    private StaffMyPayrollResponseDto calculateMyPayroll(UserStore userStore, List<WorkShift> shifts, int year, int month) {
        int hourlyWage = DEFAULT_HOURLY_WAGE;
        boolean isFiveOrMore = DEFAULT_FIVE_OR_MORE;

        // 일별로 그룹화
        Map<LocalDate, List<WorkShift>> shiftsByDate = shifts.stream()
                .collect(Collectors.groupingBy(ws -> ws.getStartDatetime().toLocalDate()));

        BigDecimal totalBasePay = BigDecimal.ZERO;
        BigDecimal totalOvertimePay = BigDecimal.ZERO;
        BigDecimal totalNightPay = BigDecimal.ZERO;
        BigDecimal totalHolidayPay = BigDecimal.ZERO;

        long totalWorkMinutes = 0;
        long totalBreakMinutes = 0;
        long totalOvertimeMinutes = 0;
        long totalNightMinutes = 0;
        long totalHolidayMinutes = 0;

        for (Map.Entry<LocalDate, List<WorkShift>> entry : shiftsByDate.entrySet()) {
            LocalDate date = entry.getKey();
            List<WorkShift> dayShifts = entry.getValue();

            long dailyWorkedMinutes = 0;
            for (WorkShift shift : dayShifts) {
                long shiftMinutes = TimeRangeUtil.calculateWorkMinutes(
                        shift.getStartDatetime(), shift.getEndDatetime(), 0);
                long breakMinutes = calculateBreakMinutes(shiftMinutes);
                totalBreakMinutes += breakMinutes;

                boolean isHoliday = date.getDayOfWeek() == DayOfWeek.SUNDAY;

                WorkTimeDto workTime = WorkTimeDto.builder()
                        .workShiftId(shift.getId())
                        .startTime(shift.getStartDatetime())
                        .endTime(shift.getEndDatetime())
                        .isHoliday(isHoliday)
                        .breakMinutes(breakMinutes)
                        .build();

                PayDetailDto payDetail = payCalculatorService.calculatePay(
                        workTime, hourlyWage, isFiveOrMore, dailyWorkedMinutes);


                totalBasePay = totalBasePay.add(payDetail.getBasePay());
                totalOvertimePay = totalOvertimePay.add(payDetail.getOvertimePay());
                totalNightPay = totalNightPay.add(payDetail.getNightPay());
                totalHolidayPay = totalHolidayPay.add(payDetail.getHolidayPay());

                totalWorkMinutes += payDetail.getTotalWorkMinutes();
                totalOvertimeMinutes += payDetail.getOvertimeMinutes();
                totalNightMinutes += payDetail.getNightWorkMinutes();
                totalHolidayMinutes += payDetail.getHolidayWorkMinutes() + payDetail.getHolidayOvertimeMinutes();

                dailyWorkedMinutes += payDetail.getTotalWorkMinutes();
            }
        }

        // 주휴수당 계산
        BigDecimal weeklyAllowance = calculateWeeklyAllowance(shifts, hourlyWage, year, month);

        BigDecimal totalPay = totalBasePay
                .add(totalOvertimePay)
                .add(totalNightPay)
                .add(totalHolidayPay)
                .add(weeklyAllowance);

        Store store = userStore.getStore();

        return StaffMyPayrollResponseDto.builder()
                .storeId(store.getId())
                .storeName(store.getName())
                .year(year)
                .month(month)
                .hourlyWage(hourlyWage)
                .totalWorkMinutes(totalWorkMinutes)
                .breakMinutes(totalBreakMinutes)
                .overtimeMinutes(totalOvertimeMinutes)
                .nightWorkMinutes(totalNightMinutes)
                .holidayWorkMinutes(totalHolidayMinutes)
                .basePay(totalBasePay)
                .overtimePay(totalOvertimePay)
                .nightPay(totalNightPay)
                .holidayPay(totalHolidayPay)
                .weeklyAllowance(weeklyAllowance)
                .totalPay(totalPay)
                .totalShiftCount(shifts.size())
                .build();
    }

    /**
     * 주휴수당 계산
     * 조건: 주 15시간 이상 근무, 결근 없음
     * 계산: (주 근로시간 ÷ 40) × 8 × 시급
     */
    private BigDecimal calculateWeeklyAllowance(List<WorkShift> shifts, int hourlyWage, int year, int month) {
        if (shifts.isEmpty()) {
            return BigDecimal.ZERO;
        }

        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate monthStart = yearMonth.atDay(1);
        LocalDate monthEnd = yearMonth.atEndOfMonth();

        BigDecimal totalWeeklyAllowance = BigDecimal.ZERO;

        // 해당 월의 각 주에 대해 주휴수당 계산
        LocalDate weekStart = monthStart.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        while (!weekStart.isAfter(monthEnd)) {
            LocalDate weekEnd = weekStart.plusDays(6);

            // 해당 주의 근무 시간 합계
            LocalDate finalWeekStart = weekStart;
            long weeklyMinutes = shifts.stream()
                    .filter(ws -> {
                        LocalDate shiftDate = ws.getStartDatetime().toLocalDate();
                        return !shiftDate.isBefore(finalWeekStart) && !shiftDate.isAfter(weekEnd);
                    })
                    .mapToLong(ws -> TimeRangeUtil.calculateWorkMinutes(
                            ws.getStartDatetime(), ws.getEndDatetime(),
                            calculateBreakMinutes(TimeRangeUtil.calculateWorkMinutes(
                                    ws.getStartDatetime(), ws.getEndDatetime(), 0))))
                    .sum();

            double weeklyHours = TimeRangeUtil.minutesToHours(weeklyMinutes);

            // 주 15시간 이상 근무 시 주휴수당 지급
            if (weeklyHours >= LaborLawConstants.WEEKLY_MIN_HOURS_FOR_WEEKLY_ALLOWANCE) {
                // 주휴수당 = (주 근로시간 ÷ 40) × 8 × 시급, 최대 8시간분
                double ratio = Math.min(weeklyHours / LaborLawConstants.WEEKLY_STANDARD_HOURS, 1.0);
                double allowanceHours = ratio * LaborLawConstants.WEEKLY_ALLOWANCE_PAID_HOURS;
                BigDecimal weekAllowance = BigDecimal.valueOf(allowanceHours * hourlyWage);
                totalWeeklyAllowance = totalWeeklyAllowance.add(weekAllowance);
            }

            weekStart = weekStart.plusWeeks(1);
        }

        return totalWeeklyAllowance.setScale(0, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * 휴게시간 계산
     * - 4시간 이상 근무: 30분
     * - 8시간 이상 근무: 60분
     */
    private long calculateBreakMinutes(long workMinutes) {
        if (workMinutes >= 8 * 60) {
            return 60;
        } else if (workMinutes >= 4 * 60) {
            return 30;
        }
        return 0;
    }
}

