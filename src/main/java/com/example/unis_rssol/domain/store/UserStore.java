package com.example.unis_rssol.domain.store;

import com.example.unis_rssol.domain.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity @Table(name="user_store",
        uniqueConstraints = @UniqueConstraint(columnNames={"user_id","store_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserStore {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="user_id") private User user;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="store_id") private Store store;

    @Enumerated(EnumType.STRING) private Position position; // OWNER or STAFF
    public enum Position { OWNER, STAFF }

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private EmploymentStatus employmentStatus = EmploymentStatus.HIRED;
    public enum EmploymentStatus { HIRED, ON_LEAVE, RESIGNED }

    @Column(name = "hire_date")
    private LocalDate hireDate;

    // 시급 (원 단위, null이면 최저임금 적용)
    @Column(name = "hourly_wage")
    private Integer hourlyWage;

    private LocalDateTime createdAt; private LocalDateTime updatedAt;
    @PrePersist void pre(){ createdAt=LocalDateTime.now(); updatedAt=LocalDateTime.now(); }
    @PreUpdate  void upd(){ updatedAt=LocalDateTime.now(); }

    /**
     * 직원 시급 업데이트
     *
     * @param hourlyWage 시급 (null이면 최저임금 적용)
     */
    public void updateHourlyWage(Integer hourlyWage) {
        this.hourlyWage = hourlyWage;
    }
}