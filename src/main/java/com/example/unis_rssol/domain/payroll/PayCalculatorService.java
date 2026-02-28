package com.example.unis_rssol.domain.payroll;

import com.example.unis_rssol.domain.payroll.dto.PayDetailDto;
import com.example.unis_rssol.domain.payroll.dto.WorkTimeDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 급여 계산 서비스
 * 한국 근로기준법 기준:
 * - 연장근무: 1일 8시간 초과 시 통상임금의 50% 가산 (5인 이상)
 * - 야간근무: 22:00~06:00 근무 시 통상임금의 50% 가산 (5인 이상)
 * - 휴일근무: 법정휴일 근무 시 통상임금의 50% 가산 (5인 이상)
 * - 휴일연장근무: 휴일 8시간 초과 시 100% 가산 (휴일 50% + 연장 50%)
 * 5인 미만 사업장: 연장/야간/휴일 가산수당 미적용 (기본급만)
 * 5인 이상 사업장: 가산수당 적용
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayCalculatorService {

    // 야간 시간대 (22:00 ~ 06:00)
    private static final LocalTime NIGHT_START = LocalTime.of(22, 0);
    private static final LocalTime NIGHT_END = LocalTime.of(6, 0);

    // 1일 법정근로시간 (분)
    private static final int DAILY_REGULAR_MINUTES = 8 * 60;

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

        // 총 근무 시간 (분) - 휴게시간 제외
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

        // 야간 근무 시간 (22:00 ~ 06:00) - 5인 이상만 계산
        long nightMinutes = isFiveOrMore ? calculateNightMinutes(start, end) : 0;

        // 휴일 근무 시간
        long holidayMinutes = 0;
        long holidayOvertimeMinutes = 0;
        if (workTime.isHoliday()) {
            holidayMinutes = Math.min(totalMinutes, DAILY_REGULAR_MINUTES);
            holidayOvertimeMinutes = Math.max(0, totalMinutes - DAILY_REGULAR_MINUTES);
        }

        // 분당 임금 계산
        BigDecimal minuteRate = BigDecimal.valueOf(hourlyWage).divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);

        // 기본급 (기준 근무시간)
        BigDecimal basePay = minuteRate.multiply(BigDecimal.valueOf(standardMinutes));

        // 연장수당 계산
        // 5인 이상: 기본급 + 50% 가산 = 1.5배
        // 5인 미만: 기본급만 = 1배
        BigDecimal overtimePay;
        if (isFiveOrMore) {
            overtimePay = minuteRate.multiply(BigDecimal.valueOf(1.5)).multiply(BigDecimal.valueOf(overtimeMinutes));
        } else {
            overtimePay = minuteRate.multiply(BigDecimal.valueOf(overtimeMinutes));
        }

        // 야간수당 (5인 이상만 50% 가산)
        // 야간 근무는 기본급에 추가로 50% 가산 (기본급은 이미 basePay에 포함)
        BigDecimal nightPay = isFiveOrMore
                ? minuteRate.multiply(BigDecimal.valueOf(0.5)).multiply(BigDecimal.valueOf(nightMinutes))
                : BigDecimal.ZERO;

        // 휴일수당 (5인 이상만 적용)
        BigDecimal holidayPay = BigDecimal.ZERO;
        if (workTime.isHoliday() && isFiveOrMore) {
            // 휴일 기본 8시간 이내: 50% 가산
            holidayPay = minuteRate.multiply(BigDecimal.valueOf(0.5)).multiply(BigDecimal.valueOf(holidayMinutes));
            // 휴일 8시간 초과분: 100% 가산 (휴일 50% + 연장 50%)
            holidayPay = holidayPay.add(
                    minuteRate.multiply(BigDecimal.valueOf(1.0)).multiply(BigDecimal.valueOf(holidayOvertimeMinutes))
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
     * 야간 근무 시간 계산 (22:00 ~ 06:00)
     *
     * @param start 근무 시작 시간
     * @param end 근무 종료 시간
     * @return 야간 근무 시간 (분)
     */
    private long calculateNightMinutes(LocalDateTime start, LocalDateTime end) {
        long nightMinutes = 0;
        LocalDateTime current = start;

        while (current.isBefore(end)) {
            LocalTime currentTime = current.toLocalTime();

            // 22:00 ~ 23:59:59 또는 00:00 ~ 06:00
            boolean isNightTime = !currentTime.isBefore(NIGHT_START) || currentTime.isBefore(NIGHT_END);

            if (isNightTime) {
                nightMinutes++;
            }
            current = current.plusMinutes(1);
        }

        return nightMinutes;
    }
}
