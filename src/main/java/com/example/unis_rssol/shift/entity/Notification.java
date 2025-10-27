package com.example.unis_rssol.shift.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private Long shiftSwapRequestId;

    @Enumerated(EnumType.STRING)
    private Type type;

    @Column(columnDefinition = "TEXT")
    private String message;

    private boolean isRead = false;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public enum Type {
        SHIFT_SWAP_REQUEST,
        SHIFT_SWAP_NOTIFY_MANAGER,
        SHIFT_SWAP_MANAGER_APPROVED_REQUESTER,
        SHIFT_SWAP_MANAGER_APPROVED_RECEIVER,
        SHIFT_SWAP_MANAGER_REJECTED_REQUESTER,
        SHIFT_SWAP_MANAGER_REJECTED_RECEIVER,
    }
}
