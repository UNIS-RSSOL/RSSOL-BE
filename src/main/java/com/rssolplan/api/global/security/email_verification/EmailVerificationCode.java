package com.rssolplan.api.global.security.email_verification;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_verification_codes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailVerificationCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(nullable = false, length = 6)
    private String code;  // 6자리 인증 코드

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean verified;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // 만료 확인 메서드
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
