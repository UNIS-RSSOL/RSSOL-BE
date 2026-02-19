package com.example.unis_rssol.domain.payroll;

import com.example.unis_rssol.domain.payroll.dto.*;
import com.example.unis_rssol.domain.schedule.generation.entity.WorkShift;
import com.example.unis_rssol.domain.schedule.workshifts.WorkShiftRepository;
import com.example.unis_rssol.domain.store.Store;
import com.example.unis_rssol.domain.store.StoreRepository;
import com.example.unis_rssol.domain.store.UserStore;
import com.example.unis_rssol.domain.store.UserStoreRepository;
import com.example.unis_rssol.domain.store.setting.StoreSetting;
import com.example.unis_rssol.domain.store.setting.StoreSettingRepository;
import com.example.unis_rssol.domain.user.User;
import com.example.unis_rssol.domain.user.UserRepository;
import com.example.unis_rssol.global.exception.NotFoundException;
import com.example.unis_rssol.global.security.AuthorizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 급여 계산 서비스
 *
 * 한국 근로기준법 기준:
 * - 연장근무: 1일 8시간, 1주 40시간 초과 시 50% 가산
 * - 야간근무: 22:00~06:00 근무 시 50% 가산
 * - 휴일근무: 법정휴일(일요일) 근무 시 50% 가산
 * - 주휴수당: 1주 15시간 이상 근무 시 유급휴일 부여 (1일 통상임금)
 *
 * 5인 미만 사업장: 연장/야간/휴일 가산수당 미적용 (기본급만)
 * 5인 이상 사업장: 가산수당 적용
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayCalculatorService {

    private final WorkShiftRepository workShiftRepository;
    private final UserStoreRepository userStoreRepository;
    private final StoreRepository storeRepository;
    private final StoreSettingRepository storeSettingRepository;
    private final MinimumWageRepository minimumWageRepository;
    private final UserRepository userRepository;
    private final AuthorizationService authService;

    // 야간 시간대 (22:00 ~ 06:00)
    private static final LocalTime NIGHT_START = LocalTime.of(22, 0);
    private static final LocalTime NIGHT_END = LocalTime.of(6, 0);

    // 1일 법정근로시간 (분)
    private static final int DAILY_REGULAR_MINUTES = 8 * 60;

    // 주휴수당 적용 기준 (주 15시간 이상)
    private static final int WEEKLY_HOLIDAY_PAY_THRESHOLD_MINUTES = 15 * 60;

    // 5인 이상 사업장 기준
    private static final int FIVE_OR_MORE_EMPLOYEES = 5;

    // 기본 최저임금 (DB에 없을 경우 fallback)
    private static final int DEFAULT_MINIMUM_WAGE = 10030; // 2025년 기준

    /**
     * 단일 근무시간에 대한 급여 계산
     *
     * @param workTime 근무시간 정보
     * @param hourlyWage 시급
     * @param isFiveOrMore 5인 이상 사업장 여부
     * @param dailyWorkedMinutes 해당일 이미 근무한 시간 (연장근무 계산용)
     * @return 급여 상세 정보
     */
    public PayDetailDto calculatePay(WorkTimeDto workTime, int hourlyWage, boolean isFiveOrMore, long dailyWorkedMinutes) {
        LocalDateTime start = workTime.getStartTime();
        LocalDateTime end = workTime.getEndTime();

        // 총 근무 시간 (분)
        long totalMinutes = Duration.between(start, end).toMinutes() - workTime.getBreakMinutes();
        if (totalMinutes < 0) totalMinutes = 0;

        // 연장근무 계산 (하루 8시간 초과분)
        long cumulativeMinutes = dailyWorkedMinutes + totalMinutes;
        long overtimeMinutes = 0;
        long standardMinutes = totalMinutes;

        if (cumulativeMinutes > DAILY_REGULAR_MINUTES) {
            if (dailyWorkedMinutes >= DAILY_REGULAR_MINUTES) {
                // 이미 8시간 초과: 전부 연장
                overtimeMinutes = totalMinutes;
                standardMinutes = 0;
            } else {
                // 이번 근무로 8시간 초과
                overtimeMinutes = cumulativeMinutes - DAILY_REGULAR_MINUTES;
                standardMinutes = totalMinutes - overtimeMinutes;
            }
        }

        // 야간 근무 시간 (22:00 ~ 06:00)
        long nightMinutes = isFiveOrMore ? calculateNightMinutes(start, end) : 0;

        // 휴일 근무 시간
        long holidayMinutes = 0;
        long holidayOvertimeMinutes = 0;
        if (workTime.isHoliday()) {
            holidayMinutes = Math.min(totalMinutes, DAILY_REGULAR_MINUTES);
            holidayOvertimeMinutes = Math.max(0, totalMinutes - DAILY_REGULAR_MINUTES);
        }

        // 급여 계산
        BigDecimal minuteRate = BigDecimal.valueOf(hourlyWage).divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);

        // 기본급 (기준 근무시간)
        BigDecimal basePay = minuteRate.multiply(BigDecimal.valueOf(standardMinutes));

        // 연장수당 (5인 이상: 1.5배, 5인 미만: 1배)
        BigDecimal overtimePay;
        if (isFiveOrMore) {
            overtimePay = minuteRate.multiply(BigDecimal.valueOf(1.5)).multiply(BigDecimal.valueOf(overtimeMinutes));
        } else {
            overtimePay = minuteRate.multiply(BigDecimal.valueOf(overtimeMinutes));
        }

        // 야간수당 (5인 이상만 0.5배 가산)
        BigDecimal nightPay = isFiveOrMore
                ? minuteRate.multiply(BigDecimal.valueOf(0.5)).multiply(BigDecimal.valueOf(nightMinutes))
                : BigDecimal.ZERO;

        // 휴일수당 (5인 이상만 0.5배 가산)
        BigDecimal holidayPay = BigDecimal.ZERO;
        if (workTime.isHoliday() && isFiveOrMore) {
            // 휴일 기본: 0.5배 가산
            holidayPay = minuteRate.multiply(BigDecimal.valueOf(0.5)).multiply(BigDecimal.valueOf(holidayMinutes));
            // 휴일 초과: 추가 0.5배 가산
            holidayPay = holidayPay.add(
                    minuteRate.multiply(BigDecimal.valueOf(0.5)).multiply(BigDecimal.valueOf(holidayOvertimeMinutes))
            );
        }

        BigDecimal totalPay = basePay.add(overtimePay).add(nightPay).add(holidayPay)
                .setScale(0, RoundingMode.HALF_UP);

        return PayDetailDto.builder()
                .hourlyWage(hourlyWage)
                .isFiveOrMoreEmployees(isFiveOrMore)
                .totalWorkMinutes(totalMinutes)
                .standardWorkMinutes(standardMinutes)
                .overtimeMinutes(overtimeMinutes)
                .nightWorkMinutes(nightMinutes)
                .holidayWorkMinutes(holidayMinutes)
                .holidayOvertimeMinutes(holidayOvertimeMinutes)
                .basePay(basePay.setScale(0, RoundingMode.HALF_UP))
                .overtimePay(overtimePay.setScale(0, RoundingMode.HALF_UP))
                .nightPay(nightPay.setScale(0, RoundingMode.HALF_UP))
                .holidayPay(holidayPay.setScale(0, RoundingMode.HALF_UP))
                .totalPay(totalPay)
                .build();
    }

    /**
     * OWNER용: 활성화된 매장의 전체 직원 급여 요약
     */
    @Transactional(readOnly = true)
    public StorePayrollSummaryDto getStorePayrollSummary(Long userId, int year, int month) {
        Long storeId = authService.getActiveStoreIdOrThrow(userId);
        return calculateStorePayroll(storeId, year, month);
    }

    /**
     * OWNER용: 특정 직원의 급여 상세 조회
     */
    @Transactional(readOnly = true)
    public EmployeePayrollDto getEmployeePayroll(Long userId, Long targetUserStoreId, int year, int month) {
        Long storeId = authService.getActiveStoreIdOrThrow(userId);

        UserStore targetUserStore = userStoreRepository.findById(targetUserStoreId)
                .orElseThrow(() -> new NotFoundException("직원 정보를 찾을 수 없습니다."));

        // 해당 직원이 이 매장 소속인지 확인
        if (!targetUserStore.getStore().getId().equals(storeId)) {
            throw new NotFoundException("해당 매장의 직원이 아닙니다.");
        }

        return calculateEmployeePayroll(targetUserStore, year, month, isOverFiveEmployees(storeId));
    }

    /**
     * STAFF용: 본인이 근무하는 모든 매장의 급여 목록
     */
    @Transactional(readOnly = true)
    public MyPayrollListDto getMyPayrolls(Long userId, int year, int month) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

        List<UserStore> myStores = userStoreRepository.findByUser_Id(userId);

        List<EmployeePayrollDto> payrolls = myStores.stream()
                .filter(us -> us.getPosition() == UserStore.Position.STAFF)
                .map(us -> calculateEmployeePayroll(us, year, month, isOverFiveEmployees(us.getStore().getId())))
                .collect(Collectors.toList());

        return MyPayrollListDto.builder()
                .userId(userId)
                .username(user.getUsername())
                .year(year)
                .month(month)
                .payrolls(payrolls)
                .build();
    }

    /**
     * 매장 전체 급여 계산
     */
    private StorePayrollSummaryDto calculateStorePayroll(Long storeId, int year, int month) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new NotFoundException("매장을 찾을 수 없습니다."));

        // STAFF 직원만 조회 (OWNER 제외)
        List<UserStore> staffList = userStoreRepository.findByStore_IdAndPosition(storeId, UserStore.Position.STAFF);
        boolean isOverFive = staffList.size() >= FIVE_OR_MORE_EMPLOYEES;

        List<EmployeePayrollDto> employeePayrolls = staffList.stream()
                .map(us -> calculateEmployeePayroll(us, year, month, isOverFive))
                .collect(Collectors.toList());

        // 합계 계산
        BigDecimal totalRegular = BigDecimal.ZERO;
        BigDecimal totalOvertime = BigDecimal.ZERO;
        BigDecimal totalNight = BigDecimal.ZERO;
        BigDecimal totalHoliday = BigDecimal.ZERO;
        BigDecimal totalWeeklyHoliday = BigDecimal.ZERO;
        BigDecimal totalPay = BigDecimal.ZERO;

        for (EmployeePayrollDto emp : employeePayrolls) {
            totalRegular = totalRegular.add(emp.getRegularPay());
            totalOvertime = totalOvertime.add(emp.getOvertimePay());
            totalNight = totalNight.add(emp.getNightPay());
            totalHoliday = totalHoliday.add(emp.getHolidayPay());
            totalWeeklyHoliday = totalWeeklyHoliday.add(emp.getWeeklyHolidayPay());
            totalPay = totalPay.add(emp.getTotalPay());
        }

        return StorePayrollSummaryDto.builder()
                .storeId(storeId)
                .storeName(store.getName())
                .year(year)
                .month(month)
                .totalEmployees(staffList.size())
                .totalRegularPay(totalRegular)
                .totalOvertimePay(totalOvertime)
                .totalNightPay(totalNight)
                .totalHolidayPay(totalHoliday)
                .totalWeeklyHolidayPay(totalWeeklyHoliday)
                .totalPay(totalPay)
                .employees(employeePayrolls)
                .build();
    }

    /**
     * 개별 직원 급여 계산
     */
    private EmployeePayrollDto calculateEmployeePayroll(UserStore userStore, int year, int month, boolean isOverFiveEmployees) {
        Long storeId = userStore.getStore().getId();
        Long userStoreId = userStore.getId();

        // 해당 월의 근무 내역 조회
        LocalDateTime monthStart = LocalDate.of(year, month, 1).atStartOfDay();
        LocalDateTime monthEnd = YearMonth.of(year, month).atEndOfMonth().atTime(23, 59, 59);

        List<WorkShift> shifts = workShiftRepository.findByStoreIdAndDateRange(storeId, monthStart, monthEnd)
                .stream()
                .filter(ws -> ws.getUserStore().getId().equals(userStoreId))
                .filter(ws -> ws.getShiftStatus() == WorkShift.ShiftStatus.SCHEDULED)
                .collect(Collectors.toList());

        // 시급 결정 (직원 설정 시급 > 최저임금)
        int hourlyWage = determineHourlyWage(userStore, LocalDate.of(year, month, 1));

        // 브레이크타임 조회
        Optional<StoreSetting> settingOpt = storeSettingRepository.findByStoreId(storeId);
        LocalTime breakStart = null;
        LocalTime breakEnd = null;
        if (settingOpt.isPresent() && settingOpt.get().isHasBreakTime()) {
            breakStart = settingOpt.get().getBreakStartTime();
            breakEnd = settingOpt.get().getBreakEndTime();
        }

        // 근무 시간 계산
        PayCalculationResult result = calculateWorkTime(shifts, breakStart, breakEnd, isOverFiveEmployees);

        // 급여 계산
        BigDecimal hourlyRate = BigDecimal.valueOf(hourlyWage);
        BigDecimal minuteRate = hourlyRate.divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);

        BigDecimal regularPay = minuteRate.multiply(BigDecimal.valueOf(result.regularMinutes));
        BigDecimal overtimePay = isOverFiveEmployees
                ? minuteRate.multiply(BigDecimal.valueOf(1.5)).multiply(BigDecimal.valueOf(result.overtimeMinutes))
                : minuteRate.multiply(BigDecimal.valueOf(result.overtimeMinutes)); // 5인 미만: 가산 없음
        BigDecimal nightPay = isOverFiveEmployees
                ? minuteRate.multiply(BigDecimal.valueOf(0.5)).multiply(BigDecimal.valueOf(result.nightMinutes)) // 야간 가산분만
                : BigDecimal.ZERO;
        BigDecimal holidayPay = isOverFiveEmployees
                ? minuteRate.multiply(BigDecimal.valueOf(0.5)).multiply(BigDecimal.valueOf(result.holidayMinutes)) // 휴일 가산분만
                : BigDecimal.ZERO;

        // 주휴수당 계산
        BigDecimal weeklyHolidayPay = calculateWeeklyHolidayPay(shifts, hourlyWage, year, month);

        BigDecimal totalPay = regularPay.add(overtimePay).add(nightPay).add(holidayPay).add(weeklyHolidayPay)
                .setScale(0, RoundingMode.HALF_UP);

        return EmployeePayrollDto.builder()
                .userStoreId(userStoreId)
                .userId(userStore.getUser().getId())
                .username(userStore.getUser().getUsername())
                .storeName(userStore.getStore().getName())
                .hourlyWage(hourlyWage)
                .totalWorkMinutes(result.totalWorkMinutes)
                .regularMinutes(result.regularMinutes)
                .overtimeMinutes(result.overtimeMinutes)
                .nightMinutes(result.nightMinutes)
                .holidayMinutes(result.holidayMinutes)
                .breakMinutes(result.breakMinutes)
                .regularPay(regularPay.setScale(0, RoundingMode.HALF_UP))
                .overtimePay(overtimePay.setScale(0, RoundingMode.HALF_UP))
                .nightPay(nightPay.setScale(0, RoundingMode.HALF_UP))
                .holidayPay(holidayPay.setScale(0, RoundingMode.HALF_UP))
                .weeklyHolidayPay(weeklyHolidayPay.setScale(0, RoundingMode.HALF_UP))
                .totalPay(totalPay)
                .build();
    }

    /**
     * 근무 시간 계산
     */
    private PayCalculationResult calculateWorkTime(List<WorkShift> shifts,
                                                   LocalTime breakStart,
                                                   LocalTime breakEnd,
                                                   boolean isOverFiveEmployees) {
        int totalWorkMinutes = 0;
        int regularMinutes = 0;
        int overtimeMinutes = 0;
        int nightMinutes = 0;
        int holidayMinutes = 0;
        int breakMinutes = 0;

        // 일별로 그룹화하여 계산 (하루 8시간 초과 연장근무 판단)
        Map<LocalDate, List<WorkShift>> shiftsByDate = shifts.stream()
                .collect(Collectors.groupingBy(ws -> ws.getStartDatetime().toLocalDate()));

        for (Map.Entry<LocalDate, List<WorkShift>> entry : shiftsByDate.entrySet()) {
            LocalDate date = entry.getKey();
            List<WorkShift> dayShifts = entry.getValue();

            int dailyWorkMinutes = 0;
            int dailyNightMinutes = 0;
            int dailyBreakMinutes = 0;

            for (WorkShift shift : dayShifts) {
                LocalDateTime start = shift.getStartDatetime();
                LocalDateTime end = shift.getEndDatetime();

                int shiftMinutes = (int) Duration.between(start, end).toMinutes();

                // 브레이크타임 제외
                if (breakStart != null && breakEnd != null) {
                    int breakOverlap = calculateBreakOverlap(start, end, breakStart, breakEnd);
                    shiftMinutes -= breakOverlap;
                    dailyBreakMinutes += breakOverlap;
                }

                dailyWorkMinutes += shiftMinutes;

                // 야간 근무 시간 계산 (22:00 ~ 06:00)
                if (isOverFiveEmployees) {
                    dailyNightMinutes += calculateNightMinutes(start, end);
                }
            }

            totalWorkMinutes += dailyWorkMinutes;
            breakMinutes += dailyBreakMinutes;
            nightMinutes += dailyNightMinutes;

            // 휴일(일요일) 체크
            boolean isHoliday = date.getDayOfWeek() == java.time.DayOfWeek.SUNDAY;
            if (isHoliday && isOverFiveEmployees) {
                holidayMinutes += dailyWorkMinutes;
            }

            // 하루 8시간 초과 → 연장근무
            if (dailyWorkMinutes > DAILY_REGULAR_MINUTES) {
                int dailyOvertime = dailyWorkMinutes - DAILY_REGULAR_MINUTES;
                overtimeMinutes += dailyOvertime;
                regularMinutes += DAILY_REGULAR_MINUTES;
            } else {
                regularMinutes += dailyWorkMinutes;
            }
        }

        return new PayCalculationResult(totalWorkMinutes, regularMinutes, overtimeMinutes,
                nightMinutes, holidayMinutes, breakMinutes);
    }

    /**
     * 야간 근무 시간 계산 (22:00 ~ 06:00)
     */
    private int calculateNightMinutes(LocalDateTime start, LocalDateTime end) {
        int nightMinutes = 0;
        LocalDateTime current = start;

        while (current.isBefore(end)) {
            LocalTime currentTime = current.toLocalTime();

            // 22:00 ~ 23:59:59 또는 00:00 ~ 06:00
            boolean isNightTime = currentTime.isAfter(NIGHT_START.minusMinutes(1)) ||
                    currentTime.isBefore(NIGHT_END);

            if (isNightTime) {
                nightMinutes++;
            }
            current = current.plusMinutes(1);
        }

        return nightMinutes;
    }

    /**
     * 브레이크타임 겹침 계산
     */
    private int calculateBreakOverlap(LocalDateTime shiftStart, LocalDateTime shiftEnd,
                                      LocalTime breakStart, LocalTime breakEnd) {
        LocalTime workStart = shiftStart.toLocalTime();
        LocalTime workEnd = shiftEnd.toLocalTime();

        // 브레이크타임과 근무시간이 겹치는지 확인
        LocalTime overlapStart = workStart.isAfter(breakStart) ? workStart : breakStart;
        LocalTime overlapEnd = workEnd.isBefore(breakEnd) ? workEnd : breakEnd;

        if (overlapStart.isBefore(overlapEnd)) {
            return (int) Duration.between(overlapStart, overlapEnd).toMinutes();
        }
        return 0;
    }

    /**
     * 주휴수당 계산
     * - 1주 15시간 이상 근무 시 적용
     * - 주휴수당 = (1주 총 근무시간 / 40) × 8 × 시급
     */
    private BigDecimal calculateWeeklyHolidayPay(List<WorkShift> shifts, int hourlyWage, int year, int month) {
        BigDecimal totalWeeklyHolidayPay = BigDecimal.ZERO;

        // 주차별로 그룹화
        Map<Integer, List<WorkShift>> shiftsByWeek = shifts.stream()
                .collect(Collectors.groupingBy(ws -> {
                    LocalDate date = ws.getStartDatetime().toLocalDate();
                    // ISO 주차 번호 사용
                    return date.get(java.time.temporal.WeekFields.ISO.weekOfYear());
                }));

        for (Map.Entry<Integer, List<WorkShift>> entry : shiftsByWeek.entrySet()) {
            List<WorkShift> weekShifts = entry.getValue();

            int weeklyMinutes = weekShifts.stream()
                    .mapToInt(ws -> (int) Duration.between(ws.getStartDatetime(), ws.getEndDatetime()).toMinutes())
                    .sum();

            // 주 15시간 이상 근무 시 주휴수당 지급
            if (weeklyMinutes >= WEEKLY_HOLIDAY_PAY_THRESHOLD_MINUTES) {
                // 주휴수당 = (주간 근무시간 / 40시간) × 8시간 × 시급
                // 최대 40시간까지만 인정
                int cappedMinutes = Math.min(weeklyMinutes, 40 * 60);
                BigDecimal weeklyHours = BigDecimal.valueOf(cappedMinutes).divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);
                BigDecimal holidayHours = weeklyHours.divide(BigDecimal.valueOf(40), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(8));
                BigDecimal weeklyPay = holidayHours.multiply(BigDecimal.valueOf(hourlyWage));
                totalWeeklyHolidayPay = totalWeeklyHolidayPay.add(weeklyPay);
            }
        }

        return totalWeeklyHolidayPay;
    }

    /**
     * 시급 결정 (직원 설정값 > 최저임금)
     */
    private int determineHourlyWage(UserStore userStore, LocalDate date) {
        if (userStore.getHourlyWage() != null && userStore.getHourlyWage() > 0) {
            return userStore.getHourlyWage();
        }

        return minimumWageRepository.findByEffectiveDate(date)
                .map(MinimumWage::getHourlyWage)
                .orElse(DEFAULT_MINIMUM_WAGE);
    }

    /**
     * 5인 이상 사업장 여부 확인
     */
    private boolean isOverFiveEmployees(Long storeId) {
        List<UserStore> staffList = userStoreRepository.findByStore_IdAndPosition(storeId, UserStore.Position.STAFF);
        return staffList.size() >= FIVE_OR_MORE_EMPLOYEES;
    }

    /**
     * 현재 최저임금 조회
     */
    public Integer getCurrentMinimumWage() {
        return minimumWageRepository.findCurrentMinimumWage()
                .map(MinimumWage::getHourlyWage)
                .orElse(DEFAULT_MINIMUM_WAGE);
    }

    /**
     * 근무 시간 계산 결과
     */
    private static class PayCalculationResult {
        int totalWorkMinutes;
        int regularMinutes;
        int overtimeMinutes;
        int nightMinutes;
        int holidayMinutes;
        int breakMinutes;

        PayCalculationResult(int totalWorkMinutes, int regularMinutes, int overtimeMinutes,
                             int nightMinutes, int holidayMinutes, int breakMinutes) {
            this.totalWorkMinutes = totalWorkMinutes;
            this.regularMinutes = regularMinutes;
            this.overtimeMinutes = overtimeMinutes;
            this.nightMinutes = nightMinutes;
            this.holidayMinutes = holidayMinutes;
            this.breakMinutes = breakMinutes;
        }
    }
}

