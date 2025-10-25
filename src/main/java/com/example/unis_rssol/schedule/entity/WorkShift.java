package com.example.unis_rssol.schedule.entity;

import com.example.unis_rssol.store.entity.UserStore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
@Getter
@Setter
@Entity
@Table(name = "work_shift")
public class WorkShift {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_store_id", nullable = false)
    private UserStore userStore;

    @ManyToOne
    @JoinColumn(name = "schedule_settings_id")
    private ScheduleSettings scheduleSettings;

    @ManyToOne
    @JoinColumn(name = "segment_id")
    private ScheduleSettingSegment segment;

    @ManyToOne
    @JoinColumn(name = "schedule_id")
    private Schedule schedule;

    @Column(nullable = false)
    private LocalDateTime startDatetime;

    @Column(nullable = false)
    private LocalDateTime endDatetime;

    private BigDecimal minWorkHours; // 비구간 근무 시 최소 근무 시간

    private Integer breakDurationMinutes = 0;

    @Enumerated(EnumType.STRING)
    private ShiftStatus shiftStatus = ShiftStatus.SCHEDULED;

    private String note;

    @CreationTimestamp
    private Timestamp createdAt;

    @UpdateTimestamp
    private Timestamp updatedAt;

    public enum ShiftStatus {
        SCHEDULED, SWAPPED, CANCELED, LATE
    }
}

