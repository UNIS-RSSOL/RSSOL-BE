package com.example.unis_rssol.schedule.entity;

import com.example.unis_rssol.store.entity.Store;
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
}
