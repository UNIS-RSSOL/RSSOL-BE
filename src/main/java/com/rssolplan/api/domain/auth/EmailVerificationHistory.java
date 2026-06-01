package com.rssolplan.api.domain.auth;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 이메일 인증 이력 Entity
 * - 인증 코드 발송 및 검증 결과 기록
 */
@Entity
@Table(name = "email_verification_history", indexes = {
        @Index(name = "idx_email", columnList = "email"),
        @Index(name = "idx_created_at", columnList = "created_at")
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailVerificationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(nullable = false, length = 6)
    private String code;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private VerificationStatus status; // SENT, VERIFIED, EXPIRED, FAILED

    @Column(length = 500)
    private String reason; // 실패 이유

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime verifiedAt;

    /**
     * 인증 상태 Enum
     */
    public enum VerificationStatus {
        SENT,       // 인증 코드 발송됨
        VERIFIED,   // 인증 성공
        EXPIRED,    // 만료됨
        FAILED      // 인증 실패
    }
}

