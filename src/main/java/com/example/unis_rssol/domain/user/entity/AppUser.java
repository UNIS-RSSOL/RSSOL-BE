package com.example.unis_rssol.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "app_users",
        uniqueConstraints = @UniqueConstraint(columnNames = {"provider", "provider_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AppUser {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;            // 카카오 닉네임
    private String email;               // 카카오 이메일
    private String profileImageUrl;     // 카카오 프로필 이미지 URL

    private String provider;            // kakao
    @Column(name = "provider_id")
    private String providerId;          // 카카오 회원 고유 ID

    @Column(name = "active_store_id")
    private Long activeStoreId;         // 현재 선택(활성)된 매장 ID

    @Column(name = "kakao_access_token", length = 500)
    private String kakaoAccessToken;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    void pre() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void upd() {
        updatedAt = LocalDateTime.now();
    }
}
