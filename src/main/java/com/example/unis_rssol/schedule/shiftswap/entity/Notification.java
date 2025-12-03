package com.example.unis_rssol.schedule.shiftswap.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
@Table(
        name = "notifications",
        indexes = {
                @Index(name="idx_notifications_user_created", columnList = "user_id, created_at"),
                @Index(name="idx_notifications_target", columnList = "target_type, target_id")
        }
)
public class Notification {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    // 공통 타겟 (딥링크/라우팅용)
    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", length = 32)
    private TargetType targetType; // SHIFT_SWAP_REQUEST | EXTRA_SHIFT_REQUEST | EXTRA_SHIFT_RESPONSE

    @Column(name = "target_id")
    private Long targetId;

    // 대타 요청 참조
    @Column(name = "shift_swap_request_id")
    private Long shiftSwapRequestId;

    // 추가 인력 요청 참조 (DB 컬럼 그대로 유지)
    @Column(name = "staffing_request_id")
    private Long extraShiftRequestId;

    // 카테고리 (프론트 필터/아이콘용)
    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private Category category; // SHIFT_SWAP | EXTRA_SHIFT

    @Enumerated(EnumType.STRING)
    @Column(length = 64, nullable = false)
    private Type type;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String message;

    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // ==========================
    // ENUMS
    // ==========================

    public enum Category {
        SHIFT_SWAP,
        EXTRA_SHIFT
    }

    public enum TargetType {
        SHIFT_SWAP_REQUEST,
        EXTRA_SHIFT_REQUEST,
        EXTRA_SHIFT_RESPONSE
    }

    public enum Type {
        // 대타 요청 관련
        SHIFT_SWAP_REQUEST,
        SHIFT_SWAP_NOTIFY_MANAGER,
        SHIFT_SWAP_MANAGER_APPROVED_REQUESTER,
        SHIFT_SWAP_MANAGER_APPROVED_RECEIVER,
        SHIFT_SWAP_MANAGER_REJECTED_REQUESTER,
        SHIFT_SWAP_MANAGER_REJECTED_RECEIVER,

        // 추가 인력 요청 관련
        EXTRA_SHIFT_REQUEST_INVITE,
        EXTRA_SHIFT_NOTIFY_MANAGER,
        EXTRA_SHIFT_MANAGER_APPROVED_WORKER,
        EXTRA_SHIFT_MANAGER_REJECTED_WORKER,
        EXTRA_SHIFT_FILLED_BROADCAST
    }
}
