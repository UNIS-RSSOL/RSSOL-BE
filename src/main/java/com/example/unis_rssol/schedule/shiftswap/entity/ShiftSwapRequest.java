package com.example.unis_rssol.schedule.shiftswap.entity;

import com.example.unis_rssol.schedule.entity.WorkShift;
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
@Table(name = "shift_swap_requests")
public class ShiftSwapRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_id", nullable = false)
    private WorkShift shift;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private UserStore requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private UserStore receiver;

    @Column(nullable = false)
    private String reason;

    @Enumerated(EnumType.STRING)
    private Status status = Status.PENDING; // 1차 상태

    @Enumerated(EnumType.STRING)
    @Column(name = "manager_approval_status")
    private ManagerApproval managerApprovalStatus = ManagerApproval.PENDING; // 사장 승인 상태

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum Status { PENDING, ACCEPTED, REJECTED }
    public enum ManagerApproval { PENDING, APPROVED, REJECTED }
}
