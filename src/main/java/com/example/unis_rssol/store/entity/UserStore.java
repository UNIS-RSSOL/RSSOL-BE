package com.example.unis_rssol.store.entity;

import com.example.unis_rssol.user.entity.AppUser;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity @Table(name="user_store",
        uniqueConstraints = @UniqueConstraint(columnNames={"user_id","store_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserStore {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="user_id") private AppUser user;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="store_id") private Store store;

    @Enumerated(EnumType.STRING) private Position position; // OWNER or STAFF
    public enum Position { OWNER, STAFF }

    @Enumerated(EnumType.STRING) private EmploymentStatus employmentStatus = EmploymentStatus.HIRED;
    public enum EmploymentStatus { HIRED, ON_LEAVE, RESIGNED }

    @Column(name = "hire_date")
    private LocalDate hireDate;

    private LocalDateTime createdAt; private LocalDateTime updatedAt;
    @PrePersist void pre(){ createdAt=LocalDateTime.now(); updatedAt=LocalDateTime.now(); }
    @PreUpdate  void upd(){ updatedAt=LocalDateTime.now(); }
}