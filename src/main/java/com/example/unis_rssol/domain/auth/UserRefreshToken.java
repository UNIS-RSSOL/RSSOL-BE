package com.example.unis_rssol.domain.auth;

import com.example.unis_rssol.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_refresh_tokens", uniqueConstraints = @UniqueConstraint(columnNames = "refresh_token"))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class UserRefreshToken {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "refresh_token", nullable = false, length = 500)
    private String refreshToken;

    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime revokedAt;

    @PrePersist void pre() { this.createdAt = LocalDateTime.now(); }
}