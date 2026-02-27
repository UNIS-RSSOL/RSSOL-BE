package com.example.unis_rssol.domain.payroll.util;

import java.time.LocalTime;

/**
 * [생성 이유]
 * 근로기준법 관련 상수를 한 곳에서 관리하기 위함.
 * 정책 변경 시 이 클래스만 수정하면 되도록 OCP 원칙 적용.
 * <p>
 * [역할]
 * - 근로기준법 관련 상수 정의 (최저임금, 법정 근로시간, 가산율 등)
 */
public final class LaborLawConstants {

    private LaborLawConstants() {
        // 인스턴스화 방지
    }

    // DB에 최저임금 데이터가 없을 때 사용하는 최후 수단 fallback 시급 (단위: 원, 2026년 기준)
    public static final int FALLBACK_MINIMUM_WAGE = 10_320;

    // 법정 근로시간
    public static final int DAILY_STANDARD_HOURS = 8;
    public static final int WEEKLY_STANDARD_HOURS = 40;
    public static final int WEEKLY_MAX_OVERTIME_HOURS = 12;

    // 야간근로 시간대 (22:00 ~ 06:00)
    public static final LocalTime NIGHT_START = LocalTime.of(22, 0);
    public static final LocalTime NIGHT_END = LocalTime.of(6, 0);

    // 가산 배율
    public static final double STANDARD_RATE = 1.0;
    public static final double OVERTIME_RATE = 1.5;       // 연장근로 50% 가산
    public static final double NIGHT_RATE = 1.5;          // 야간근로 50% 가산
    public static final double HOLIDAY_RATE = 1.5;        // 휴일근로(8시간 이내) 50% 가산
    public static final double HOLIDAY_OVERTIME_RATE = 2.0; // 휴일근로(8시간 초과) 100% 가산

    // 중복 가산 배율
    public static final double OVERTIME_NIGHT_RATE = 2.0;      // 연장 + 야간 (50% + 50%)
    public static final double HOLIDAY_NIGHT_RATE = 2.0;       // 휴일 + 야간 (50% + 50%)
    public static final double HOLIDAY_OVERTIME_NIGHT_RATE = 2.5; // 휴일초과 + 야간 (100% + 50%)

    // 5인 이상 사업장 기준
    public static final int FIVE_OR_MORE_EMPLOYEES = 5;

    // 주휴수당 조건
    public static final int WEEKLY_MIN_HOURS_FOR_WEEKLY_ALLOWANCE = 15;
    public static final int WEEKLY_ALLOWANCE_PAID_HOURS = 8;
}

