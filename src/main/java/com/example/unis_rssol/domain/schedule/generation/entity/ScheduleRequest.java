package com.example.unis_rssol.domain.schedule.generation.entity;

import com.example.unis_rssol.domain.store.Store;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Timestamp;
import java.time.LocalDate;

/**
 * 스케줄 생성 요청 엔티티
 * - 각 스케줄 생성 요청의 상태를 관리
 * - 시간대는 StoreSetting에서, 인원수는 Redis에서 조회
 */
@Entity
@Table(name = "schedule_request")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScheduleRequestStatus status;

    // 시간대별 인원수 설정 Redis 키
    @Column(name = "temporary_setting_key")
    private String temporarySettingKey;

    // 생성된 후보 스케줄 Redis 키
    @Column(name = "candidate_schedule_key")
    private String candidateScheduleKey;

    // 최종 확정된 Schedule 참조
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id")
    private Schedule schedule;

    @CreationTimestamp
    private Timestamp createdAt;

    @UpdateTimestamp
    private Timestamp updatedAt;

    public enum ScheduleRequestStatus {
        REQUESTED,   // 근무 가능 시간 입력 요청 중
        GENERATED,   // 후보 스케줄 생성 완료
        CONFIRMED    // 최종 확정
    }
}

