package com.example.unis_rssol.schedule.extrashift.entity;

import com.example.unis_rssol.domain.store.entity.Store;
import com.example.unis_rssol.domain.store.entity.UserStore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "staffing_requests",
        indexes = {
                @Index(name="idx_staffreq_store_status_created", columnList = "store_id,status,created_at"),
                @Index(name="idx_staffreq_created", columnList = "created_at")
        })
public class ExtrashiftRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 어떤 매장의 요청인지
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id")
    private Store store;

    // 요청 생성자(사장)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id")
    private UserStore owner;

    // 기준 근무 ID (work_shift와 연결)
    @Column(name = "base_shift_id")
    private Long baseShiftId;

    @Column(name = "receiver_user_ids")
    private String receiverUserIds;

    @Column(name = "start_datetime", nullable = false)
    private LocalDateTime startDatetime;

    @Column(name = "end_datetime", nullable = false)
    private LocalDateTime endDatetime;

    @Column(name = "headcount_requested", nullable = false)
    private int headcountRequested;

    @Column(name = "headcount_filled", nullable = false)
    private int headcountFilled;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status;

    @Column(columnDefinition = "TEXT")
    private String note;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    public enum Status {
        OPEN, FILLED, CANCELLED, EXPIRED
    }
}
