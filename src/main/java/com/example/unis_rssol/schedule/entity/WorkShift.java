package com.example.unis_rssol.schedule.entity;

import com.example.unis_rssol.store.entity.Store;
import com.example.unis_rssol.store.entity.UserStore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
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
    @JoinColumn(name = "store_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Store store;

    @ManyToOne
    @JoinColumn(name = "user_store_id", nullable = false)
    @JsonProperty("user_store_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private UserStore userStore;

    @ManyToOne
    @JoinColumn(name = "schedule_id")
    private Schedule schedule;

    @Column(nullable = false)
    private LocalDateTime startDatetime;

    @Column(nullable = false)
    private LocalDateTime endDatetime;

    @Enumerated(EnumType.STRING)
    private ShiftStatus shiftStatus = ShiftStatus.SCHEDULED;

    @CreationTimestamp
    private Timestamp createdAt;

    @UpdateTimestamp
    private Timestamp updatedAt;

    public enum ShiftStatus {
        SCHEDULED, SWAPPED, CANCELED, LATE
    }
}

