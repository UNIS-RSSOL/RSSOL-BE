package com.example.unis_rssol.domain.payroll.util;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static com.example.unis_rssol.domain.payroll.util.LaborLawConstants.NIGHT_END;
import static com.example.unis_rssol.domain.payroll.util.LaborLawConstants.NIGHT_START;

/**
 * [생성 이유]
 * 시간 범위 계산 로직을 분리하여 재사용성과 테스트 용이성을 높이기 위함.
 * SRP 원칙에 따라 시간 계산 전용 유틸리티로 분리.
 * <p>
 * [역할]
 * - 야간 근무 시간 계산
 * - 시간 범위 겹침 계산
 */
public final class TimeRangeUtil {

    private TimeRangeUtil() {
        // 인스턴스화 방지
    }

    /**
     * 주어진 근무 시간 내 야간근로 시간(분)을 계산한다.
     * 야간 시간대: 22:00 ~ 06:00
     *
     * @param startTime 근무 시작 시간
     * @param endTime   근무 종료 시간
     * @return 야간 근로 시간 (분)
     */
    public static long calculateNightMinutes(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime.isAfter(endTime) || startTime.isEqual(endTime)) {
            return 0;
        }

        long totalNightMinutes = 0;
        LocalDateTime current = startTime;

        while (current.isBefore(endTime)) {
            LocalDateTime dayEnd = getNextDayBoundary(current, endTime);
            totalNightMinutes += calculateNightMinutesForDay(current, dayEnd);
            current = dayEnd;
        }

        return totalNightMinutes;
    }

    /**
     * 하루 내에서 야간 근로 시간 계산
     */
    private static long calculateNightMinutesForDay(LocalDateTime start, LocalDateTime end) {
        LocalTime startTime = start.toLocalTime();
        LocalTime endTime = end.toLocalTime();

        // 같은 날짜 내 계산
        if (start.toLocalDate().equals(end.toLocalDate())) {
            return calculateNightMinutesSameDay(startTime, endTime);
        }

        // 날짜가 다른 경우 (자정을 넘기는 경우)
        long nightMinutes = 0;

        // 첫째 날 (start ~ 자정): 22:00 ~ 24:00 구간 계산
        nightMinutes += calculateNightMinutesFirstDay(startTime);

        // 둘째 날 (자정 ~ end): 00:00 ~ 06:00 구간 계산
        nightMinutes += calculateNightMinutesSecondDay(endTime);

        return nightMinutes;
    }

    /**
     * 같은 날 내 야간 시간 계산
     */
    private static long calculateNightMinutesSameDay(LocalTime start, LocalTime end) {
        long nightMinutes = 0;

        // 00:00 ~ 06:00 구간
        if (start.isBefore(NIGHT_END)) {
            LocalTime overlapEnd = end.isBefore(NIGHT_END) ? end : NIGHT_END;
            if (start.isBefore(overlapEnd)) {
                nightMinutes += Duration.between(start, overlapEnd).toMinutes();
            }
        }

        // 22:00 ~ 24:00 구간
        if (end.isAfter(NIGHT_START) || end.equals(LocalTime.MIDNIGHT)) {
            LocalTime overlapStart = start.isAfter(NIGHT_START) ? start : NIGHT_START;
            LocalTime overlapEnd = end.equals(LocalTime.MIDNIGHT) ? LocalTime.MAX : end;
            if (overlapStart.isBefore(overlapEnd)) {
                nightMinutes += Duration.between(overlapStart, overlapEnd).toMinutes();
            }
        }

        return nightMinutes;
    }

    /**
     * 첫째 날 야간 시간 (22:00 ~ 자정)
     */
    private static long calculateNightMinutesFirstDay(LocalTime startTime) {
        if (startTime.isBefore(NIGHT_START)) {
            // 22:00부터 자정까지 = 120분
            return 120;
        } else {
            // startTime부터 자정까지
            return Duration.between(startTime, LocalTime.MAX).toMinutes() + 1;
        }
    }

    /**
     * 둘째 날 야간 시간 (자정 ~ 06:00)
     */
    private static long calculateNightMinutesSecondDay(LocalTime endTime) {
        if (endTime.isAfter(NIGHT_END) || endTime.equals(NIGHT_END)) {
            // 자정부터 06:00까지 = 360분
            return 360;
        } else {
            // 자정부터 endTime까지
            return Duration.between(LocalTime.MIDNIGHT, endTime).toMinutes();
        }
    }

    /**
     * 다음 날 경계 계산
     */
    private static LocalDateTime getNextDayBoundary(LocalDateTime current, LocalDateTime end) {
        LocalDateTime nextMidnight = current.toLocalDate().plusDays(1).atStartOfDay();
        return nextMidnight.isBefore(end) ? nextMidnight : end;
    }

    /**
     * 총 근무 시간(분) 계산 (휴게시간 제외)
     *
     * @param startTime    근무 시작 시간
     * @param endTime      근무 종료 시간
     * @param breakMinutes 휴게시간 (분)
     * @return 실제 근무 시간 (분)
     */
    public static long calculateWorkMinutes(LocalDateTime startTime, LocalDateTime endTime, long breakMinutes) {
        if (startTime.isAfter(endTime) || startTime.isEqual(endTime)) {
            return 0;
        }
        long totalMinutes = Duration.between(startTime, endTime).toMinutes();
        return Math.max(0, totalMinutes - breakMinutes);
    }

    /**
     * 분을 시간으로 변환 (소수점 포함)
     */
    public static double minutesToHours(long minutes) {
        return minutes / 60.0;
    }
}

