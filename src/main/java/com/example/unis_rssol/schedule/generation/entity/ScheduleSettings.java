package com.example.unis_rssol.schedule.generation.entity;

import com.example.unis_rssol.domain.store.entity.Store;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.sql.Timestamp;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "schedule_settings",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"store_id"})} // store_id 유니크
)
@Getter
@Setter
@RequiredArgsConstructor
public class ScheduleSettings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScheduleStatus status; //현재 근무표생성 요청상태

    @Column(nullable = false)
    private LocalTime openTime;

    @Column(nullable = false)
    private LocalTime closeTime;

    @Column(nullable = false)
    private Boolean isCategorized = false;

    @OneToMany(mappedBy = "scheduleSettings", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ScheduleSettingSegment> segments = new ArrayList<>();

    @CreationTimestamp
    private Timestamp createdAt;

    @UpdateTimestamp
    private Timestamp updatedAt;

    public ScheduleSettings(Store store, LocalTime openTime, LocalTime closeTime) {
        this.store = store;
        this.openTime = openTime;
        this.closeTime = closeTime;
    }


    public enum ScheduleStatus {
        REQUESTED,   // 입력 요청 중
        GENERATED,   // 생성 완료
        CONFIRMED    // 최종 확정
    }
}
