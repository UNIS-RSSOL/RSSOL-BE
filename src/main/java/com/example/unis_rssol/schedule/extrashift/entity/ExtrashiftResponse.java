package com.example.unis_rssol.schedule.extrashift.entity;

import com.example.unis_rssol.domain.store.entity.UserStore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
@Table(name = "staffing_responses",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_staffresp_request_candidate",
                        columnNames = {"staffing_request_id", "candidate_id"})
        },
        indexes = {
                @Index(name="idx_staffresp_request", columnList = "staffing_request_id"),
                @Index(name="idx_staffresp_request_approval", columnList = "staffing_request_id,manager_approval"),
                @Index(name="idx_staffresp_candidate", columnList = "candidate_id")
        })
public class ExtrashiftResponse {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 어떤 요청에 대한 응답인지
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "staffing_request_id")
    private ExtrashiftRequest staffingRequest;

    // 응답자(알바) - user_store 기준
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidate_id")
    private UserStore candidate;

    @Enumerated(EnumType.STRING)
    @Column(name = "worker_action", nullable = false, length = 16)
    private WorkerAction workerAction; // NONE/ACCEPT/REJECT

    @Enumerated(EnumType.STRING)
    @Column(name = "manager_approval", nullable = false, length = 16)
    private ManagerApproval managerApproval; // PENDING/APPROVED/REJECTED

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum WorkerAction { NONE, ACCEPT, REJECT }
    public enum ManagerApproval { PENDING, APPROVED, REJECTED }
}
