package com.example.unis_rssol.domain.schedule.attendance;

public enum AttendanceStatus {
    BEFORE_WORK,    // 출근 전
    WORKING,        // 근무 중
    FINISHED,       // 퇴근 완료
    NO_SCHEDULE,    // 스케줄 없음
    LATE,           // 지각
    ABSENT          // 결근
}
