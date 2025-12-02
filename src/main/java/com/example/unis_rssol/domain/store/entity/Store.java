package com.example.unis_rssol.domain.store.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name="store")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Store {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private String storeCode;         // UNIQUE
    private String name;
    private String address;
    private String phoneNumber;
    private String businessRegistrationNumber;
    @Column(columnDefinition="json") private String settings;

    private LocalDateTime createdAt; private LocalDateTime updatedAt;
    @PrePersist void pre(){ createdAt=LocalDateTime.now(); updatedAt=LocalDateTime.now(); }
    @PreUpdate  void upd(){ updatedAt=LocalDateTime.now(); }
}