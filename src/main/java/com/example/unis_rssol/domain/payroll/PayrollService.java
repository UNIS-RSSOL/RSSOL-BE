package com.example.unis_rssol.domain.payroll;

import com.example.unis_rssol.domain.bank.BankAccount;
import com.example.unis_rssol.domain.bank.BankAccountRepository;
import com.example.unis_rssol.domain.payroll.dto.*;
import com.example.unis_rssol.domain.payroll.util.LaborLawConstants;
import com.example.unis_rssol.domain.payroll.util.TimeRangeUtil;
import com.example.unis_rssol.domain.schedule.attendance.Attendance;
import com.example.unis_rssol.domain.schedule.attendance.AttendanceRepository;
import com.example.unis_rssol.domain.schedule.generation.entity.WorkShift;
import com.example.unis_rssol.domain.schedule.workshifts.WorkShiftRepository;
import com.example.unis_rssol.domain.store.Store;
import com.example.unis_rssol.domain.store.StoreRepository;
import com.example.unis_rssol.domain.store.UserStore;
import com.example.unis_rssol.domain.store.UserStoreRepository;
import com.example.unis_rssol.domain.store.dto.AllStaffSummaryResponseDto;
import com.example.unis_rssol.domain.store.dto.StaffSummaryDto;
import com.example.unis_rssol.domain.user.User;
import com.example.unis_rssol.global.exception.ForbiddenException;
import com.example.unis_rssol.global.exception.NotFoundException;
import com.example.unis_rssol.global.security.AuthorizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
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
    private final MinimumWageRepository minimumWageRepository;
    private final StoreRepository storeRepository;
    private final AttendanceRepository attendanceRepository;
    private final BankAccountRepository bankAccountRepository;

    // 5인 이상 사업장 여부 (추후 Store 설정으로 관리 가능)
    private static final boolean DEFAULT_FIVE_OR_MORE = true;

    /**
     * OWNER: 활성 매장의 전체 직원 급여 조회
     */
    @Transactional(readOnly = true)
    public OwnerPayrollSummaryDto getStorePayrollSummary(Long userId, int year, int month) {
        Long storeId = authorizationService.getActiveStoreIdOrThrow(userId);

        log.info("userId={}, storeId={}", userId, storeId);


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
     * OWNER: 매장 전체 직원 요약 목록 조회
     * 직원관리 화면에서 사용 - 이름, 역할, 이번달 급여, 연락처, 계좌, 지각/결근 횟수
     */
    @Transactional(readOnly = true)
    public AllStaffSummaryResponseDto getAllStaffSummary(Long userId, int year, int month) {
        Long storeId = authorizationService.getActiveStoreIdOrThrow(userId);

        // OWNER 권한 확인
        UserStore ownerUserStore = userStoreRepository.findByUser_IdAndStore_Id(userId, storeId)
                .orElseThrow(() -> new ForbiddenException("해당 매장에 대한 권한이 없습니다."));

        if (ownerUserStore.getPosition() != UserStore.Position.OWNER) {
            throw new ForbiddenException("직원 목록 조회는 OWNER만 가능합니다.");
        }

        Store store = ownerUserStore.getStore();

        // 해당 월의 시작/종료 시간
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDateTime monthStart = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime monthEnd = yearMonth.atEndOfMonth().plusDays(1).atStartOfDay();

        // 매장의 모든 직원 조회 (OWNER 포함)
        List<UserStore> allStaff = userStoreRepository.findByStore_Id(storeId);

        // 해당 월의 모든 WorkShift 조회
        List<WorkShift> allShifts = workShiftRepository.findByStoreIdAndMonthRange(storeId, monthStart, monthEnd);

        // UserStore별로 그룹화
        Map<Long, List<WorkShift>> shiftsByUserStore = allShifts.stream()
                .collect(Collectors.groupingBy(ws -> ws.getUserStore().getId()));

        // 각 직원별 요약 정보 생성
        List<StaffSummaryDto> staffSummaries = new ArrayList<>();

        for (UserStore staff : allStaff) {
            User user = staff.getUser();

            // 급여 계산
            List<WorkShift> staffShifts = shiftsByUserStore.getOrDefault(staff.getId(), Collections.emptyList());
            StaffPayrollResponseDto payroll = calculateStaffPayroll(staff, staffShifts, year, month);

            // 은행 계좌 정보 조회
            BankAccount bankAccount = bankAccountRepository.findTopByUserIdOrderByIdDesc(user.getId())
                    .orElse(null);

            String bankName = null;
            String accountNumber = null;
            if (bankAccount != null) {
                bankName = bankAccount.getBank() != null ? bankAccount.getBank().getBankName() : null;
                accountNumber = bankAccount.getAccountNumber();
            }

            StaffSummaryDto summary = StaffSummaryDto.builder()
                    .userStoreId(staff.getId())
                    .username(user.getUsername())
                    .profileImageUrl(user.getProfileImageUrl())
                    .role(staff.getPosition().name())
                    .employmentStatus(staff.getEmploymentStatus().name())
                    .monthlyPay(payroll.getTotalPay())
                    .email(user.getEmail())
                    .tel(null) // 현재 User 엔티티에 전화번호 필드 없음
                    .bankName(bankName)
                    .accountNumber(accountNumber)
                    .lateCount(payroll.getLateCount())
                    .absenceCount(payroll.getAbsenceCount())
                    .totalShiftCount(payroll.getTotalShiftCount())
                    .build();

            staffSummaries.add(summary);
        }

        log.info("📋 [직원목록] OWNER userId={} 매장 storeId={} 전체 직원 {} 명 조회 - {}/{}",
                userId, storeId, staffSummaries.size(), year, month);

        return AllStaffSummaryResponseDto.builder()
                .storeId(storeId)
                .storeName(store.getName())
                .year(year)
                .month(month)
                .totalStaffCount(staffSummaries.size())
                .staffList(staffSummaries)
                .build();
    }

    // ==================== 매장 전체 인건비 합산 (StorePayrollSummaryDto) ====================

    /**
     * OWNER: 매장 전체 급여 요약 조회 (인건비 합산)
     * - 매장의 모든 직원 급여 합산
     * - 직원별 급여 상세 목록 포함
     */
    @Transactional(readOnly = true)
    public StorePayrollSummaryDto getStorePayrollTotal(Long userId, int year, int month) {
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
        List<EmployeePayrollDto> employeePayrolls = new ArrayList<>();
        BigDecimal totalRegularPay = BigDecimal.ZERO;
        BigDecimal totalOvertimePay = BigDecimal.ZERO;
        BigDecimal totalNightPay = BigDecimal.ZERO;
        BigDecimal totalHolidayPay = BigDecimal.ZERO;
        BigDecimal totalWeeklyHolidayPay = BigDecimal.ZERO;
        BigDecimal grandTotalPay = BigDecimal.ZERO;

        for (UserStore staff : staffList) {
            List<WorkShift> staffShifts = shiftsByUserStore.getOrDefault(staff.getId(), Collections.emptyList());
            EmployeePayrollDto employeePayroll = calculateEmployeePayroll(staff, staffShifts, year, month);
            employeePayrolls.add(employeePayroll);

            totalRegularPay = totalRegularPay.add(employeePayroll.getRegularPay());
            totalOvertimePay = totalOvertimePay.add(employeePayroll.getOvertimePay());
            totalNightPay = totalNightPay.add(employeePayroll.getNightPay());
            totalHolidayPay = totalHolidayPay.add(employeePayroll.getHolidayPay());
            totalWeeklyHolidayPay = totalWeeklyHolidayPay.add(employeePayroll.getWeeklyHolidayPay());
            grandTotalPay = grandTotalPay.add(employeePayroll.getTotalPay());
        }

        log.info("📊 [매장인건비] OWNER userId={} 매장 storeId={} 총 인건비: {}원 - {}/{}",
                userId, storeId, grandTotalPay, year, month);

        return StorePayrollSummaryDto.builder()
                .storeId(storeId)
                .storeName(store.getName())
                .year(year)
                .month(month)
                .totalEmployees(staffList.size())
                .totalRegularPay(totalRegularPay)
                .totalOvertimePay(totalOvertimePay)
                .totalNightPay(totalNightPay)
                .totalHolidayPay(totalHolidayPay)
                .totalWeeklyHolidayPay(totalWeeklyHolidayPay)
                .totalPay(grandTotalPay)
                .employees(employeePayrolls)
                .build();
    }

    // ==================== 내가 속한 모든 매장의 급여 합산 (MyPayrollListDto) ====================

    /**
     * STAFF: 내가 속한 모든 매장의 급여 조회 (합산 포함)
     */
    @Transactional(readOnly = true)
    public MyPayrollListDto getMyPayrollsTotal(Long userId, int year, int month) {
        User user = userStoreRepository.findByUser_Id(userId).stream()
                .findFirst()
                .map(UserStore::getUser)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

        // 사용자가 속한 모든 UserStore 조회
        List<UserStore> myUserStores = userStoreRepository.findByUser_Id(userId);

        // 해당 월의 시작/종료 시간
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDateTime monthStart = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime monthEnd = yearMonth.atEndOfMonth().plusDays(1).atStartOfDay();

        List<EmployeePayrollDto> payrolls = new ArrayList<>();
        BigDecimal grandTotalPay = BigDecimal.ZERO;

        for (UserStore userStore : myUserStores) {
            List<WorkShift> shifts = workShiftRepository.findByUserStoreIdAndMonthRange(
                    userStore.getId(), monthStart, monthEnd);

            EmployeePayrollDto payroll = calculateEmployeePayroll(userStore, shifts, year, month);
            payrolls.add(payroll);
            grandTotalPay = grandTotalPay.add(payroll.getTotalPay());
        }

        log.info("📊 [내급여] userId={} 총 매장 {} 개, 총 급여: {}원 - {}/{}",
                userId, myUserStores.size(), grandTotalPay, year, month);

        return MyPayrollListDto.builder()
                .userId(userId)
                .username(user.getUsername())
                .profileImageUrl(user.getProfileImageUrl())
                .year(year)
                .month(month)
                .grandTotalPay(grandTotalPay)
                .payrolls(payrolls)
                .build();
    }

    // ==================== 특정 직원 급여 상세 (EmployeePayrollDto) ====================

    /**
     * OWNER: 특정 직원의 급여 상세 조회 (EmployeePayrollDto 형식)
     */
    @Transactional(readOnly = true)
    public EmployeePayrollDto getEmployeePayrollDetail(Long userId, Long userStoreId, int year, int month) {
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

        return calculateEmployeePayroll(targetStaff, staffShifts, year, month);
    }

    /**
     * 개별 직원 급여 계산 (EmployeePayrollDto 형식)
     * 출석(Attendance) 데이터를 반영하여 지각/결근 분을 고려
     */
    private EmployeePayrollDto calculateEmployeePayroll(UserStore staff, List<WorkShift> shifts, int year, int month) {
        User user = staff.getUser();
        int hourlyWage = staff.getHourlyWage() != null ? staff.getHourlyWage() : getMinimumHourlyWage(year, month);
        boolean isFiveOrMore = DEFAULT_FIVE_OR_MORE;

        // 해당 월의 출석 데이터 조회
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate monthStart = yearMonth.atDay(1);
        LocalDate monthEnd = yearMonth.atEndOfMonth();

        List<Attendance> attendances = attendanceRepository.findByUserStoreIdAndWorkDateBetween(
                staff.getId(), monthStart, monthEnd);

        // WorkShift ID별 출석 데이터 매핑
        Map<Long, Attendance> attendanceByShiftId = attendances.stream()
                .filter(a -> a.getWorkShiftId() != null)
                .collect(Collectors.toMap(Attendance::getWorkShiftId, a -> a, (a1, a2) -> a1));

        // 날짜별 출석 데이터 매핑
        Map<LocalDate, Attendance> attendanceByDate = attendances.stream()
                .collect(Collectors.toMap(Attendance::getWorkDate, a -> a, (a1, a2) -> a1));

        // 일별로 그룹화하여 연장근로 계산
        Map<LocalDate, List<WorkShift>> shiftsByDate = shifts.stream()
                .collect(Collectors.groupingBy(ws -> ws.getStartDatetime().toLocalDate()));

        BigDecimal totalRegularPay = BigDecimal.ZERO;
        BigDecimal totalOvertimePay = BigDecimal.ZERO;
        BigDecimal totalNightPay = BigDecimal.ZERO;
        BigDecimal totalHolidayPay = BigDecimal.ZERO;

        long totalWorkMinutes = 0;
        long totalRegularMinutes = 0;
        long totalOvertimeMinutes = 0;
        long totalNightMinutes = 0;
        long totalHolidayMinutes = 0;
        long totalBreakMinutes = 0;
        long totalLateMinutes = 0;
        int lateCount = 0;
        int absenceCount = 0;

        for (Map.Entry<LocalDate, List<WorkShift>> entry : shiftsByDate.entrySet()) {
            LocalDate date = entry.getKey();
            List<WorkShift> dayShifts = entry.getValue();

            long dailyWorkedMinutes = 0;
            for (WorkShift shift : dayShifts) {
                // 출석 데이터 확인
                Attendance attendance = attendanceByShiftId.get(shift.getId());
                if (attendance == null) {
                    attendance = attendanceByDate.get(date);
                }

                // 결근 체크: 출석 기록이 없거나 체크인하지 않은 경우 (과거 날짜만)
                if (date.isBefore(LocalDate.now())) {
                    if (attendance == null || !attendance.isCheckedIn()) {
                        absenceCount++;
                        continue;
                    }
                }

                // 실제 근무 시작/종료 시간 결정
                LocalDateTime actualStart = shift.getStartDatetime();
                LocalDateTime actualEnd = shift.getEndDatetime();
                long shiftLateMinutes = 0;

                if (attendance != null && attendance.isCheckedIn()) {
                    // 지각 계산
                    if (attendance.getCheckInTime() != null &&
                            attendance.getCheckInTime().isAfter(shift.getStartDatetime())) {
                        actualStart = attendance.getCheckInTime();
                        shiftLateMinutes = Duration.between(shift.getStartDatetime(), actualStart).toMinutes();
                        if (shiftLateMinutes > 0) {
                            lateCount++;
                            totalLateMinutes += shiftLateMinutes;
                        }
                    }

                    // 조기 퇴근 처리
                    if (attendance.isCheckedOut() && attendance.getCheckOutTime() != null &&
                            attendance.getCheckOutTime().isBefore(shift.getEndDatetime())) {
                        actualEnd = attendance.getCheckOutTime();
                    }
                }

                // 실제 근무 시간이 유효한지 확인
                if (!actualEnd.isAfter(actualStart)) {
                    continue;
                }

                // 휴게시간 계산
                long shiftMinutes = TimeRangeUtil.calculateWorkMinutes(actualStart, actualEnd, 0);
                long breakMinutes = calculateBreakMinutes(shiftMinutes);
                totalBreakMinutes += breakMinutes;

                // 휴일 여부
                boolean isHoliday = date.getDayOfWeek() == DayOfWeek.SUNDAY;

                WorkTimeDto workTime = WorkTimeDto.builder()
                        .workShiftId(shift.getId())
                        .startTime(actualStart)
                        .endTime(actualEnd)
                        .isHoliday(isHoliday)
                        .breakMinutes(breakMinutes)
                        .build();

                PayDetailDto payDetail = payCalculatorService.calculatePay(
                        workTime, hourlyWage, isFiveOrMore, dailyWorkedMinutes);

                totalRegularPay = totalRegularPay.add(payDetail.getBasePay());
                totalOvertimePay = totalOvertimePay.add(payDetail.getOvertimePay());
                totalNightPay = totalNightPay.add(payDetail.getNightPay());
                totalHolidayPay = totalHolidayPay.add(payDetail.getHolidayPay());

                totalWorkMinutes += payDetail.getTotalWorkMinutes();
                totalRegularMinutes += payDetail.getStandardWorkMinutes();
                totalOvertimeMinutes += payDetail.getOvertimeMinutes();
                totalNightMinutes += payDetail.getNightWorkMinutes();
                totalHolidayMinutes += payDetail.getHolidayWorkMinutes() + payDetail.getHolidayOvertimeMinutes();

                dailyWorkedMinutes += payDetail.getTotalWorkMinutes();
            }
        }

        // 주휴수당 계산
        BigDecimal weeklyHolidayPay = calculateWeeklyAllowanceWithAbsence(
                shifts, attendanceByShiftId, attendanceByDate, hourlyWage, year, month);

        BigDecimal totalPay = totalRegularPay
                .add(totalOvertimePay)
                .add(totalNightPay)
                .add(totalHolidayPay)
                .add(weeklyHolidayPay);

        return EmployeePayrollDto.builder()
                .userStoreId(staff.getId())
                .userId(user.getId())
                .username(user.getUsername())
                .profileImageUrl(user.getProfileImageUrl())
                .storeName(staff.getStore().getName())
                .hourlyWage(hourlyWage)
                .totalWorkMinutes(totalWorkMinutes)
                .regularMinutes(totalRegularMinutes)
                .overtimeMinutes(totalOvertimeMinutes)
                .nightMinutes(totalNightMinutes)
                .holidayMinutes(totalHolidayMinutes)
                .breakMinutes(totalBreakMinutes)
                .lateMinutes(totalLateMinutes)
                .regularPay(totalRegularPay)
                .overtimePay(totalOvertimePay)
                .nightPay(totalNightPay)
                .holidayPay(totalHolidayPay)
                .weeklyHolidayPay(weeklyHolidayPay)
                .totalPay(totalPay)
                .totalShiftCount(shifts.size())
                .lateCount(lateCount)
                .absenceCount(absenceCount)
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
     * 출석(Attendance) 데이터를 반영하여 지각/결근 분을 고려
     */
    private StaffPayrollResponseDto calculateStaffPayroll(UserStore staff, List<WorkShift> shifts, int year, int month) {
        int hourlyWage = staff.getHourlyWage() != null ? staff.getHourlyWage() : getMinimumHourlyWage(year, month);
        boolean isFiveOrMore = DEFAULT_FIVE_OR_MORE;

        // 해당 월의 출석 데이터 조회
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate monthStart = yearMonth.atDay(1);
        LocalDate monthEnd = yearMonth.atEndOfMonth();

        List<Attendance> attendances = attendanceRepository.findByUserStoreIdAndWorkDateBetween(
                staff.getId(), monthStart, monthEnd);

        // WorkShift ID별 출석 데이터 매핑
        Map<Long, Attendance> attendanceByShiftId = attendances.stream()
                .filter(a -> a.getWorkShiftId() != null)
                .collect(Collectors.toMap(Attendance::getWorkShiftId, a -> a, (a1, a2) -> a1));

        // 날짜별 출석 데이터 매핑 (WorkShiftId가 없는 경우 대비)
        Map<LocalDate, Attendance> attendanceByDate = attendances.stream()
                .collect(Collectors.toMap(Attendance::getWorkDate, a -> a, (a1, a2) -> a1));

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
        long totalLateMinutes = 0;
        int lateCount = 0;
        int absenceCount = 0;

        for (Map.Entry<LocalDate, List<WorkShift>> entry : shiftsByDate.entrySet()) {
            LocalDate date = entry.getKey();
            List<WorkShift> dayShifts = entry.getValue();

            long dailyWorkedMinutes = 0;
            for (WorkShift shift : dayShifts) {
                // 출석 데이터 확인
                Attendance attendance = attendanceByShiftId.get(shift.getId());
                if (attendance == null) {
                    attendance = attendanceByDate.get(date);
                }

                // 결근 체크: 출석 기록이 없거나 체크인하지 않은 경우 (과거 날짜만)
                if (date.isBefore(LocalDate.now())) {
                    if (attendance == null || !attendance.isCheckedIn()) {
                        absenceCount++;
                        continue; // 결근 시 해당 근무에 대해 급여 미지급
                    }
                }

                // 실제 근무 시작/종료 시간 결정
                LocalDateTime actualStart = shift.getStartDatetime();
                LocalDateTime actualEnd = shift.getEndDatetime();
                long lateMinutes = 0;

                if (attendance != null && attendance.isCheckedIn()) {
                    // 지각 계산: 출석 체크인 시간이 근무 시작 시간보다 늦은 경우
                    if (attendance.getCheckInTime() != null &&
                            attendance.getCheckInTime().isAfter(shift.getStartDatetime())) {
                        actualStart = attendance.getCheckInTime();
                        lateMinutes = Duration.between(shift.getStartDatetime(), actualStart).toMinutes();
                        if (lateMinutes > 0) {
                            lateCount++;
                            totalLateMinutes += lateMinutes;
                        }
                    }

                    // 조기 퇴근 처리: 체크아웃 시간이 근무 종료 시간보다 빠른 경우
                    if (attendance.isCheckedOut() && attendance.getCheckOutTime() != null &&
                            attendance.getCheckOutTime().isBefore(shift.getEndDatetime())) {
                        actualEnd = attendance.getCheckOutTime();
                    }
                }

                // 실제 근무 시간이 유효한지 확인
                if (!actualEnd.isAfter(actualStart)) {
                    continue;
                }

                // 휴게시간 계산 (4시간 이상 30분, 8시간 이상 1시간)
                long shiftMinutes = TimeRangeUtil.calculateWorkMinutes(actualStart, actualEnd, 0);
                long breakMinutes = calculateBreakMinutes(shiftMinutes);
                totalBreakMinutes += breakMinutes;

                // 휴일 여부 (일요일을 주휴일로 가정)
                boolean isHoliday = date.getDayOfWeek() == DayOfWeek.SUNDAY;

                WorkTimeDto workTime = WorkTimeDto.builder()
                        .workShiftId(shift.getId())
                        .startTime(actualStart)
                        .endTime(actualEnd)
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

        // 주휴수당 계산 (결근이 있으면 해당 주 주휴수당 미지급 고려)
        BigDecimal weeklyAllowance = calculateWeeklyAllowanceWithAbsence(shifts, attendanceByShiftId, attendanceByDate, hourlyWage, year, month);

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
                .lateMinutes(totalLateMinutes)
                .basePay(totalBasePay)
                .overtimePay(totalOvertimePay)
                .nightPay(totalNightPay)
                .holidayPay(totalHolidayPay)
                .weeklyAllowance(weeklyAllowance)
                .totalPay(totalPay)
                .totalShiftCount(shifts.size())
                .lateCount(lateCount)
                .absenceCount(absenceCount)
                .build();
    }

    /**
     * 본인 급여 계산 (STAFF 조회용)
     * 출석(Attendance) 데이터를 반영하여 지각/결근 분을 고려
     */
    private StaffMyPayrollResponseDto calculateMyPayroll(UserStore userStore, List<WorkShift> shifts, int year, int month) {
        int hourlyWage = userStore.getHourlyWage() != null ? userStore.getHourlyWage() : getMinimumHourlyWage(year, month);
        boolean isFiveOrMore = DEFAULT_FIVE_OR_MORE;

        // 해당 월의 출석 데이터 조회
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate monthStart = yearMonth.atDay(1);
        LocalDate monthEnd = yearMonth.atEndOfMonth();

        List<Attendance> attendances = attendanceRepository.findByUserStoreIdAndWorkDateBetween(
                userStore.getId(), monthStart, monthEnd);

        // WorkShift ID별 출석 데이터 매핑
        Map<Long, Attendance> attendanceByShiftId = attendances.stream()
                .filter(a -> a.getWorkShiftId() != null)
                .collect(Collectors.toMap(Attendance::getWorkShiftId, a -> a, (a1, a2) -> a1));

        // 날짜별 출석 데이터 매핑
        Map<LocalDate, Attendance> attendanceByDate = attendances.stream()
                .collect(Collectors.toMap(Attendance::getWorkDate, a -> a, (a1, a2) -> a1));

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
        long totalLateMinutes = 0;
        int lateCount = 0;
        int absenceCount = 0;

        for (Map.Entry<LocalDate, List<WorkShift>> entry : shiftsByDate.entrySet()) {
            LocalDate date = entry.getKey();
            List<WorkShift> dayShifts = entry.getValue();

            long dailyWorkedMinutes = 0;
            for (WorkShift shift : dayShifts) {
                // 출석 데이터 확인
                Attendance attendance = attendanceByShiftId.get(shift.getId());
                if (attendance == null) {
                    attendance = attendanceByDate.get(date);
                }

                // 결근 체크: 출석 기록이 없거나 체크인하지 않은 경우 (과거 날짜만)
                if (date.isBefore(LocalDate.now())) {
                    if (attendance == null || !attendance.isCheckedIn()) {
                        absenceCount++;
                        continue; // 결근 시 해당 근무에 대해 급여 미지급
                    }
                }

                // 실제 근무 시작/종료 시간 결정
                LocalDateTime actualStart = shift.getStartDatetime();
                LocalDateTime actualEnd = shift.getEndDatetime();
                long lateMinutes = 0;

                if (attendance != null && attendance.isCheckedIn()) {
                    // 지각 계산
                    if (attendance.getCheckInTime() != null &&
                            attendance.getCheckInTime().isAfter(shift.getStartDatetime())) {
                        actualStart = attendance.getCheckInTime();
                        lateMinutes = Duration.between(shift.getStartDatetime(), actualStart).toMinutes();
                        if (lateMinutes > 0) {
                            lateCount++;
                            totalLateMinutes += lateMinutes;
                        }
                    }

                    // 조기 퇴근 처리
                    if (attendance.isCheckedOut() && attendance.getCheckOutTime() != null &&
                            attendance.getCheckOutTime().isBefore(shift.getEndDatetime())) {
                        actualEnd = attendance.getCheckOutTime();
                    }
                }

                // 실제 근무 시간이 유효한지 확인
                if (!actualEnd.isAfter(actualStart)) {
                    continue;
                }

                long shiftMinutes = TimeRangeUtil.calculateWorkMinutes(actualStart, actualEnd, 0);
                long breakMinutes = calculateBreakMinutes(shiftMinutes);
                totalBreakMinutes += breakMinutes;

                boolean isHoliday = date.getDayOfWeek() == DayOfWeek.SUNDAY;

                WorkTimeDto workTime = WorkTimeDto.builder()
                        .workShiftId(shift.getId())
                        .startTime(actualStart)
                        .endTime(actualEnd)
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

        // 주휴수당 계산 (결근 고려)
        BigDecimal weeklyAllowance = calculateWeeklyAllowanceWithAbsence(shifts, attendanceByShiftId, attendanceByDate, hourlyWage, year, month);

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
                .lateMinutes(totalLateMinutes)
                .basePay(totalBasePay)
                .overtimePay(totalOvertimePay)
                .nightPay(totalNightPay)
                .holidayPay(totalHolidayPay)
                .weeklyAllowance(weeklyAllowance)
                .totalPay(totalPay)
                .totalShiftCount(shifts.size())
                .lateCount(lateCount)
                .absenceCount(absenceCount)
                .build();
    }

    /**
     * 주휴수당 계산 (결근 고려)
     *
     * 한국 근로기준법 기준:
     * - 조건: 주 15시간 이상 근무, 해당 주에 결근 없음
     * - 계산: (주 소정근로시간 ÷ 40) × 8 × 시급, 최대 8시간분
     */
    private BigDecimal calculateWeeklyAllowanceWithAbsence(
            List<WorkShift> shifts,
            Map<Long, Attendance> attendanceByShiftId,
            Map<LocalDate, Attendance> attendanceByDate,
            int hourlyWage,
            int year,
            int month) {

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
            LocalDate finalWeekStart = weekStart;

            // 해당 주의 근무 목록
            List<WorkShift> weekShifts = shifts.stream()
                    .filter(ws -> {
                        LocalDate shiftDate = ws.getStartDatetime().toLocalDate();
                        return !shiftDate.isBefore(finalWeekStart) && !shiftDate.isAfter(weekEnd);
                    })
                    .toList();

            // 해당 주에 결근이 있는지 확인
            boolean hasAbsence = false;
            long actualWeeklyMinutes = 0;

            for (WorkShift shift : weekShifts) {
                LocalDate shiftDate = shift.getStartDatetime().toLocalDate();

                // 과거 날짜만 결근 판정
                if (shiftDate.isBefore(LocalDate.now())) {
                    Attendance attendance = attendanceByShiftId.get(shift.getId());
                    if (attendance == null) {
                        attendance = attendanceByDate.get(shiftDate);
                    }

                    if (attendance == null || !attendance.isCheckedIn()) {
                        hasAbsence = true;
                        break; // 결근이 있으면 해당 주 주휴수당 미지급
                    }

                    // 실제 근무 시간 계산 (출석 기반)
                    LocalDateTime actualStart = shift.getStartDatetime();
                    LocalDateTime actualEnd = shift.getEndDatetime();

                    if (attendance.getCheckInTime() != null &&
                            attendance.getCheckInTime().isAfter(shift.getStartDatetime())) {
                        actualStart = attendance.getCheckInTime();
                    }
                    if (attendance.isCheckedOut() && attendance.getCheckOutTime() != null &&
                            attendance.getCheckOutTime().isBefore(shift.getEndDatetime())) {
                        actualEnd = attendance.getCheckOutTime();
                    }

                    if (actualEnd.isAfter(actualStart)) {
                        long shiftMinutes = TimeRangeUtil.calculateWorkMinutes(actualStart, actualEnd, 0);
                        long breakMinutes = calculateBreakMinutes(shiftMinutes);
                        actualWeeklyMinutes += (shiftMinutes - breakMinutes);
                    }
                } else {
                    // 미래 날짜는 예정된 근무시간으로 계산
                    long shiftMinutes = TimeRangeUtil.calculateWorkMinutes(
                            shift.getStartDatetime(), shift.getEndDatetime(), 0);
                    long breakMinutes = calculateBreakMinutes(shiftMinutes);
                    actualWeeklyMinutes += (shiftMinutes - breakMinutes);
                }
            }

            // 결근이 없고 주 15시간 이상 근무 시 주휴수당 지급
            if (!hasAbsence) {
                double weeklyHours = TimeRangeUtil.minutesToHours(actualWeeklyMinutes);

                if (weeklyHours >= LaborLawConstants.WEEKLY_MIN_HOURS_FOR_WEEKLY_ALLOWANCE) {
                    double ratio = Math.min(weeklyHours / LaborLawConstants.WEEKLY_STANDARD_HOURS, 1.0);
                    double allowanceHours = ratio * LaborLawConstants.WEEKLY_ALLOWANCE_PAID_HOURS;
                    BigDecimal weekAllowance = BigDecimal.valueOf(allowanceHours * hourlyWage);
                    totalWeeklyAllowance = totalWeeklyAllowance.add(weekAllowance);
                }
            }

            weekStart = weekStart.plusWeeks(1);
        }

        return totalWeeklyAllowance.setScale(0, RoundingMode.HALF_UP);
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

    // ==================== Admin: 최저임금 관리 ====================

    /**
     * Admin: 최저임금 등록/수정
     */
    @Transactional
    public MinimumWage updateMinimumWage(MinimumWageUpdateDto dto) {
        log.info("⚙️ [Admin] 최저임금 업데이트 - {}원 (적용: {} ~ {})",
                dto.getHourlyWage(), dto.getEffectiveFrom(), dto.getEffectiveTo());

        // 기존 레코드가 있으면 종료일 설정
        minimumWageRepository.findCurrentMinimumWage().ifPresent(current -> {
            if (dto.getEffectiveFrom() != null && current.getEffectiveFrom().isBefore(dto.getEffectiveFrom())) {
                current.updateWageInfo(
                        current.getHourlyWage(),
                        current.getEffectiveFrom(),
                        dto.getEffectiveFrom().minusDays(1),
                        current.getDescription()
                );
                minimumWageRepository.save(current);
            }
        });

        // 새 최저임금 등록
        MinimumWage newWage = MinimumWage.builder()
                .hourlyWage(dto.getHourlyWage())
                .effectiveFrom(dto.getEffectiveFrom() != null ? dto.getEffectiveFrom() : LocalDate.now())
                .effectiveTo(dto.getEffectiveTo())
                .description(dto.getDescription())
                .build();

        return minimumWageRepository.save(newWage);
    }

    /**
     * 특정 연도의 최저임금 조회
     */
    @Transactional(readOnly = true)
    public MinimumWage getMinimumWage(int year) {
        return minimumWageRepository.findByYear(year)
                .orElseGet(() -> {
                    log.warn("⚠️ {}년 최저임금 데이터 없음, 기본값 반환", year);
                    return MinimumWage.builder()
                            .hourlyWage(LaborLawConstants.FALLBACK_MINIMUM_WAGE)
                            .effectiveFrom(LocalDate.of(year, 1, 1))
                            .description("기본 최저임금")
                            .build();
                });
    }

    /**
     * 현재 적용 중인 최저임금 조회
     */
    @Transactional(readOnly = true)
    public MinimumWage getCurrentMinimumWage() {
        return minimumWageRepository.findCurrentMinimumWage()
                .orElseGet(() -> MinimumWage.builder()
                        .hourlyWage(LaborLawConstants.FALLBACK_MINIMUM_WAGE)
                        .effectiveFrom(LocalDate.now())
                        .description("기본 최저임금")
                        .build());
    }

    // ==================== Owner: 직원 시급 관리 ====================

    /**
     * Owner: 직원 시급 설정
     */
    @Transactional
    public void updateStaffWage(Long ownerId, Long userStoreId, Integer hourlyWage) {
        Long storeId = authorizationService.getActiveStoreIdOrThrow(ownerId);

        // OWNER 권한 확인
        UserStore ownerUserStore = userStoreRepository.findByUser_IdAndStore_Id(ownerId, storeId)
                .orElseThrow(() -> new ForbiddenException("해당 매장에 대한 권한이 없습니다."));

        if (ownerUserStore.getPosition() != UserStore.Position.OWNER) {
            throw new ForbiddenException("직원 시급 설정은 OWNER만 가능합니다.");
        }

        // 대상 직원 조회
        UserStore targetStaff = userStoreRepository.findById(userStoreId)
                .orElseThrow(() -> new NotFoundException("해당 직원을 찾을 수 없습니다."));

        // 해당 매장 소속 직원인지 확인
        if (!targetStaff.getStore().getId().equals(storeId)) {
            throw new ForbiddenException("해당 직원은 이 매장 소속이 아닙니다.");
        }

        targetStaff.updateHourlyWage(hourlyWage);
        userStoreRepository.save(targetStaff);

        log.info("💰 [시급설정] 직원 userStoreId={} 시급 변경: {}원", userStoreId, hourlyWage);
    }

    /**
     * Owner: 매장 전체 직원 시급 목록 조회
     */
    @Transactional(readOnly = true)
    public StoreStaffWagesResponseDto getAllStaffWages(Long ownerId) {
        Long storeId = authorizationService.getActiveStoreIdOrThrow(ownerId);

        // OWNER 권한 확인
        UserStore ownerUserStore = userStoreRepository.findByUser_IdAndStore_Id(ownerId, storeId)
                .orElseThrow(() -> new ForbiddenException("해당 매장에 대한 권한이 없습니다."));

        if (ownerUserStore.getPosition() != UserStore.Position.OWNER) {
            throw new ForbiddenException("직원 시급 조회는 OWNER만 가능합니다.");
        }

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new NotFoundException("매장을 찾을 수 없습니다."));

        // 현재 최저임금 조회
        MinimumWage currentMinWage = getCurrentMinimumWage();
        int minimumWage = currentMinWage.getHourlyWage();

        // 매장의 모든 STAFF 조회
        List<UserStore> staffList = userStoreRepository.findByStore_IdAndPosition(storeId, UserStore.Position.STAFF);

        List<StaffWageInfoDto> wageInfos = staffList.stream()
                .map(staff -> {
                    Integer staffWage = staff.getHourlyWage();
                    int effectiveWage = (staffWage != null) ? staffWage : minimumWage;

                    return StaffWageInfoDto.builder()
                            .userStoreId(staff.getId())
                            .staffName(staff.getUser().getUsername())
                            .hourlyWage(staffWage)
                            .effectiveWage(effectiveWage)
                            .build();
                })
                .collect(Collectors.toList());

        return StoreStaffWagesResponseDto.builder()
                .storeId(storeId)
                .storeName(store.getName())
                .currentMinimumWage(minimumWage)
                .staffWages(wageInfos)
                .build();
    }

    /**
     * 특정 연월에 적용되는 최저임금 시급을 동적으로 조회한다.
     * 해당 날짜의 적용 최저임금 → 현재 유효 최저임금 → 법정 기준값 순으로 fallback.
     */
    private int getMinimumHourlyWage(int year, int month) {
        LocalDate targetDate = LocalDate.of(year, month, 1);
        return minimumWageRepository.findByEffectiveDate(targetDate)
                .map(MinimumWage::getHourlyWage)
                .orElseGet(() -> minimumWageRepository.findCurrentMinimumWage()
                        .map(MinimumWage::getHourlyWage)
                        .orElse(LaborLawConstants.FALLBACK_MINIMUM_WAGE));
    }
}

