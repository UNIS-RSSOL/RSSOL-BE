package com.example.unis_rssol.domain.schedule.attendance;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "attendance",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_attendance_day",
                        columnNames = {"user_store_id", "work_date"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_store_id", nullable = false)
    private Long userStoreId;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Column(name = "work_shift_id")
    private Long workShiftId;

    @Column(name = "is_checked_in", nullable = false)
    private boolean isCheckedIn;

    @Column(name = "check_in_time")
    private LocalDateTime checkInTime;

    @Column(name = "is_checked_out", nullable = false)
    private boolean isCheckedOut;

    @Column(name = "check_out_time")
    private LocalDateTime checkOutTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttendanceStatus status;

    @Column(name = "created_at", updatable = false, insertable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false)
    private LocalDateTime updatedAt;

    // 출퇴근 관련 비즈니스 메서드들

    public void checkIn(LocalDateTime now) {
        this.isCheckedIn = true;
        this.checkInTime = now;
        this.status = AttendanceStatus.WORKING;
    }

    public void checkOut(LocalDateTime now) {
        this.isCheckedOut = true;
        this.checkOutTime = now;
        this.status = AttendanceStatus.FINISHED;
    }
}
