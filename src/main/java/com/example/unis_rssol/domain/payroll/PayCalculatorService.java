package com.example.unis_rssol.domain.payroll;

import com.example.unis_rssol.domain.payroll.dto.PayDetailDto;
import com.example.unis_rssol.domain.payroll.dto.WorkTimeDto;
import com.example.unis_rssol.domain.payroll.util.TimeRangeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static com.example.unis_rssol.domain.payroll.util.LaborLawConstants.*;

/**
 * [생성 이유]
 * 수당 계산 로직을 Service 계층에서 담당.
 * 5인 이상 사업장 조건에 따른 가산수당 적용 여부 및
 * 연장/야간/휴일 수당의 중복 가산 규칙을 처리함.
 * <p>
 * [역할]
 * - 연장근로수당 (Overtime Pay) 계산
 * - 야간근로수당 (Night Pay) 계산
 * - 휴일근로수당 (Holiday Pay) 계산
 * - 중복 가산 규칙 적용
 */
@Slf4j
@Service
public class PayCalculatorService {

    /**
     * 단일 근무(WorkShift)에 대한 수당 계산
     *
     * @param workTime              근무 시간 정보
     * @param hourlyWage            시급
     * @param isFiveOrMoreEmployees 5인 이상 사업장 여부
     * @param dailyWorkedMinutes    해당 일자에 이미 근무한 시간 (분) - 일일 연장 판단용
     * @return 수당 계산 결과
     */
    public PayDetailDto calculatePay(
            WorkTimeDto workTime,
            int hourlyWage,
            boolean isFiveOrMoreEmployees,
            long dailyWorkedMinutes
    ) {


        // 기본 근무 시간 계산 (휴게시간 제외)
        long totalWorkMinutes = TimeRangeUtil.calculateWorkMinutes(
                workTime.getStartTime(),
                workTime.getEndTime(),
                workTime.getBreakMinutes()
        );


        // 야간 근로 시간 계산
        long nightWorkMinutes = TimeRangeUtil.calculateNightMinutes(
                workTime.getStartTime(),
                workTime.getEndTime()
        );

        // 5인 미만 사업장: 기본급만 계산
        if (!isFiveOrMoreEmployees) {
            return buildBasicPayDetail(totalWorkMinutes, hourlyWage, false);
        }

        // 5인 이상 사업장: 가산수당 적용
        if (workTime.isHoliday()) {
            return calculateHolidayPay(totalWorkMinutes, nightWorkMinutes, hourlyWage);
        } else {
            return calculateRegularPay(totalWorkMinutes, nightWorkMinutes, hourlyWage, dailyWorkedMinutes);
        }


    }

    /**
     * 일반일(비휴일) 수당 계산 - 연장/야간 가산 적용
     */
    private PayDetailDto calculateRegularPay(
            long totalWorkMinutes,
            long nightWorkMinutes,
            int hourlyWage,
            long dailyWorkedMinutes
    ) {
        long dailyStandardMinutes = DAILY_STANDARD_HOURS * 60L;

        // 누적 근무시간 기준으로 연장근로 판단
        long totalDailyMinutes = dailyWorkedMinutes + totalWorkMinutes;
        long overtimeMinutes = Math.max(0, totalDailyMinutes - dailyStandardMinutes);

        // 이 근무에서 실제 연장에 해당하는 시간
        long thisShiftOvertimeMinutes = Math.min(overtimeMinutes, totalWorkMinutes);
        long standardWorkMinutes = totalWorkMinutes - thisShiftOvertimeMinutes;

        // 야간 시간 중 연장에 해당하는 부분 계산 (중복 가산용)
        long overtimeNightMinutes = calculateOverlapMinutes(thisShiftOvertimeMinutes, nightWorkMinutes, totalWorkMinutes);
        long standardNightMinutes = nightWorkMinutes - overtimeNightMinutes;

        // 수당 계산
        BigDecimal basePay = calculateBasePay(standardWorkMinutes, standardNightMinutes, hourlyWage);
        BigDecimal overtimePay = calculateOvertimePay(thisShiftOvertimeMinutes, overtimeNightMinutes, hourlyWage);
        BigDecimal nightPay = calculateNightPay(standardNightMinutes, hourlyWage);

        BigDecimal totalPay = basePay.add(overtimePay).add(nightPay);

        return PayDetailDto.builder()
                .hourlyWage(hourlyWage)
                .isFiveOrMoreEmployees(true)
                .totalWorkMinutes(totalWorkMinutes)
                .standardWorkMinutes(standardWorkMinutes)
                .overtimeMinutes(thisShiftOvertimeMinutes)
                .nightWorkMinutes(nightWorkMinutes)
                .overtimeNightMinutes(overtimeNightMinutes)
                .holidayWorkMinutes(0)
                .holidayOvertimeMinutes(0)
                .holidayNightMinutes(0)
                .holidayOvertimeNightMinutes(0)
                .basePay(basePay)
                .overtimePay(overtimePay)
                .nightPay(nightPay)
                .holidayPay(BigDecimal.ZERO)
                .totalPay(totalPay)
                .appliedRatesDescription(buildRatesDescription(false, thisShiftOvertimeMinutes > 0, nightWorkMinutes > 0))
                .build();
    }

    /**
     * 휴일 수당 계산 - 휴일/야간 가산 적용 (8시간 초과시 100% 가산)
     */
    private PayDetailDto calculateHolidayPay(
            long totalWorkMinutes,
            long nightWorkMinutes,
            int hourlyWage
    ) {
        long holidayStandardMinutes = DAILY_STANDARD_HOURS * 60L;

        // 휴일 8시간 초과분 계산
        long holidayOvertimeMinutes = Math.max(0, totalWorkMinutes - holidayStandardMinutes);
        long holidayRegularMinutes = totalWorkMinutes - holidayOvertimeMinutes;

        // 야간 시간 중 휴일초과에 해당하는 부분
        long holidayOvertimeNightMinutes = calculateOverlapMinutes(holidayOvertimeMinutes, nightWorkMinutes, totalWorkMinutes);
        long holidayRegularNightMinutes = nightWorkMinutes - holidayOvertimeNightMinutes;

        // 수당 계산
        // 1. 휴일 기본 (8시간 이내): 시급 × 1.5
        BigDecimal holidayBasePay = calculateHolidayBasePay(holidayRegularMinutes, holidayRegularNightMinutes, hourlyWage);

        // 2. 휴일 초과 (8시간 초과): 시급 × 2.0
        BigDecimal holidayOvertimePay = calculateHolidayOvertimePay(holidayOvertimeMinutes, holidayOvertimeNightMinutes, hourlyWage);

        // 3. 야간 가산 (휴일과 중복 적용)
        BigDecimal nightPay = calculateHolidayNightPay(nightWorkMinutes, holidayOvertimeNightMinutes, hourlyWage);

        BigDecimal totalHolidayPay = holidayBasePay.add(holidayOvertimePay).add(nightPay);

        return PayDetailDto.builder()
                .hourlyWage(hourlyWage)
                .isFiveOrMoreEmployees(true)
                .totalWorkMinutes(totalWorkMinutes)
                .standardWorkMinutes(0)
                .overtimeMinutes(0)
                .nightWorkMinutes(nightWorkMinutes)
                .holidayWorkMinutes(holidayRegularMinutes)
                .holidayOvertimeMinutes(holidayOvertimeMinutes)
                .overtimeNightMinutes(0)
                .holidayNightMinutes(holidayRegularNightMinutes)
                .holidayOvertimeNightMinutes(holidayOvertimeNightMinutes)
                .basePay(BigDecimal.ZERO)
                .overtimePay(BigDecimal.ZERO)
                .nightPay(nightPay)
                .holidayPay(holidayBasePay.add(holidayOvertimePay))
                .totalPay(totalHolidayPay)
                .appliedRatesDescription(buildRatesDescription(true, holidayOvertimeMinutes > 0, nightWorkMinutes > 0))
                .build();
    }

    /**
     * 기본급 계산 (야간 제외 표준 근무 + 야간 표준 근무)
     */
    private BigDecimal calculateBasePay(long standardWorkMinutes, long standardNightMinutes, int hourlyWage) {
        // 야간이 아닌 표준 근무: 시급 × 1.0
        long nonNightStandardMinutes = standardWorkMinutes - standardNightMinutes;
        BigDecimal nonNightPay = calculatePayForMinutes(nonNightStandardMinutes, hourlyWage, STANDARD_RATE);

        // 야간 표준 근무: 시급 × 1.5 (기본급 + 야간가산 포함)
        BigDecimal nightBasePay = calculatePayForMinutes(standardNightMinutes, hourlyWage, NIGHT_RATE);

        return nonNightPay.add(nightBasePay);
    }

    /**
     * 연장수당 계산 (야간 중복 시 2.0배율 적용)
     */
    private BigDecimal calculateOvertimePay(long overtimeMinutes, long overtimeNightMinutes, int hourlyWage) {
        // 야간이 아닌 연장: 시급 × 1.5
        long nonNightOvertimeMinutes = overtimeMinutes - overtimeNightMinutes;
        BigDecimal nonNightOvertimePay = calculatePayForMinutes(nonNightOvertimeMinutes, hourlyWage, OVERTIME_RATE);

        // 야간 연장 (중복): 시급 × 2.0
        BigDecimal nightOvertimePay = calculatePayForMinutes(overtimeNightMinutes, hourlyWage, OVERTIME_NIGHT_RATE);

        return nonNightOvertimePay.add(nightOvertimePay);
    }

    /**
     * 야간수당 계산 (표준 근무 시간 내 야간분에 대한 가산)
     * 이미 basePay에 포함되었으므로 여기서는 0 반환
     */
    private BigDecimal calculateNightPay(long standardNightMinutes, int hourlyWage) {
        // 야간 가산은 이미 basePay에서 1.5 배율로 계산됨
        // 별도 항목으로 분리하려면 0.5 가산분만 여기서 계산
        return BigDecimal.ZERO;
    }

    /**
     * 휴일 기본급 계산 (8시간 이내)
     */
    private BigDecimal calculateHolidayBasePay(long holidayRegularMinutes, long holidayRegularNightMinutes, int hourlyWage) {
        // 휴일 비야간: 시급 × 1.5
        long nonNightMinutes = holidayRegularMinutes - holidayRegularNightMinutes;
        BigDecimal nonNightPay = calculatePayForMinutes(nonNightMinutes, hourlyWage, HOLIDAY_RATE);

        // 휴일 + 야간 (중복): 시급 × 2.0
        BigDecimal nightPay = calculatePayForMinutes(holidayRegularNightMinutes, hourlyWage, HOLIDAY_NIGHT_RATE);

        return nonNightPay.add(nightPay);
    }

    /**
     * 휴일 초과수당 계산 (8시간 초과분)
     */
    private BigDecimal calculateHolidayOvertimePay(long holidayOvertimeMinutes, long holidayOvertimeNightMinutes, int hourlyWage) {
        // 휴일 초과 비야간: 시급 × 2.0
        long nonNightMinutes = holidayOvertimeMinutes - holidayOvertimeNightMinutes;
        BigDecimal nonNightPay = calculatePayForMinutes(nonNightMinutes, hourlyWage, HOLIDAY_OVERTIME_RATE);

        // 휴일 초과 + 야간 (중복): 시급 × 2.5
        BigDecimal nightPay = calculatePayForMinutes(holidayOvertimeNightMinutes, hourlyWage, HOLIDAY_OVERTIME_NIGHT_RATE);

        return nonNightPay.add(nightPay);
    }

    /**
     * 휴일 야간 가산 (별도 항목)
     * 이미 holidayBasePay와 holidayOvertimePay에 포함되어 있으므로 0 반환
     */
    private BigDecimal calculateHolidayNightPay(long totalNightMinutes, long holidayOvertimeNightMinutes, int hourlyWage) {
        return BigDecimal.ZERO;
    }

    /**
     * 분 단위 시간에 대한 급여 계산
     */
    private BigDecimal calculatePayForMinutes(long minutes, int hourlyWage, double rate) {
        if (minutes <= 0) {
            return BigDecimal.ZERO;
        }
        double hours = TimeRangeUtil.minutesToHours(minutes);
        return BigDecimal.valueOf(hours)
                .multiply(BigDecimal.valueOf(hourlyWage))
                .multiply(BigDecimal.valueOf(rate))
                .setScale(0, RoundingMode.HALF_UP);
    }

    /**
     * 5인 미만 사업장용 기본급만 계산
     */
    private PayDetailDto buildBasicPayDetail(long totalWorkMinutes, int hourlyWage, boolean isFiveOrMore) {
        BigDecimal basePay = calculatePayForMinutes(totalWorkMinutes, hourlyWage, STANDARD_RATE);

        return PayDetailDto.builder()
                .hourlyWage(hourlyWage)
                .isFiveOrMoreEmployees(isFiveOrMore)
                .totalWorkMinutes(totalWorkMinutes)
                .standardWorkMinutes(totalWorkMinutes)
                .overtimeMinutes(0)
                .nightWorkMinutes(0)
                .holidayWorkMinutes(0)
                .holidayOvertimeMinutes(0)
                .overtimeNightMinutes(0)
                .holidayNightMinutes(0)
                .holidayOvertimeNightMinutes(0)
                .basePay(basePay)
                .overtimePay(BigDecimal.ZERO)
                .nightPay(BigDecimal.ZERO)
                .holidayPay(BigDecimal.ZERO)
                .totalPay(basePay)
                .appliedRatesDescription("5인 미만 사업장 - 기본급만 적용 (가산수당 미적용)")
                .build();
    }

    /**
     * 두 시간 구간의 겹침 계산 (비율 기반)
     * 야간 시간이 연장/휴일초과 시간과 겹치는 부분 계산용
     */
    private long calculateOverlapMinutes(long segmentMinutes, long nightMinutes, long totalMinutes) {
        if (totalMinutes == 0 || segmentMinutes == 0 || nightMinutes == 0) {
            return 0;
        }
        // 비율 기반으로 야간 시간 중 segment에 해당하는 부분 계산
        double ratio = (double) segmentMinutes / totalMinutes;
        return Math.round(nightMinutes * ratio);
    }

    /**
     * 적용 배율 설명 문자열 생성
     */
    private String buildRatesDescription(boolean isHoliday, boolean hasOvertime, boolean hasNight) {
        StringBuilder sb = new StringBuilder();
        sb.append("5인 이상 사업장 - ");

        if (isHoliday) {
            sb.append("휴일근로(×1.5)");
            if (hasOvertime) {
                sb.append(", 휴일초과(×2.0)");
            }
            if (hasNight) {
                sb.append(", 휴일+야간 중복(×2.0/×2.5)");
            }
        } else {
            sb.append("기본급(×1.0)");
            if (hasOvertime) {
                sb.append(", 연장(×1.5)");
            }
            if (hasNight) {
                sb.append(", 야간(×1.5)");
            }
            if (hasOvertime && hasNight) {
                sb.append(", 연장+야간 중복(×2.0)");
            }
        }

        return sb.toString();
    }
}

