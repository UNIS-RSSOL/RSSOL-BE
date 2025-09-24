package com.example.unis_rssol.user.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name="app_users",
        uniqueConstraints = @UniqueConstraint(columnNames={"provider","provider_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AppUser {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    private String username;
    private String phoneNumber;

    private String provider;               // kakao/naver/google
    @Column(name="provider_id") private String providerId;

    @Enumerated(EnumType.STRING) private Status status = Status.ACTIVE;
    public enum Status { ACTIVE, DORMANT, WITHDRAWN }

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist void pre(){ createdAt=LocalDateTime.now(); updatedAt=LocalDateTime.now(); }
    @PreUpdate  void upd(){ updatedAt=LocalDateTime.now(); }
}