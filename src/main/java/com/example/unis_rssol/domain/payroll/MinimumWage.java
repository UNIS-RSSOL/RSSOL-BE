package com.example.unis_rssol.domain.payroll;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Timestamp;
import java.time.LocalDate;

/**
 * 최저임금 관리 Entity
 * - 매년 최저임금이 변경될 때마다 새 레코드 추가
 * - 유효기간(effectiveFrom ~ effectiveTo)으로 관리
 */
@Entity
@Table(name = "minimum_wage")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MinimumWage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 시급 (원 단위)
    @Column(nullable = false)
    private Integer hourlyWage;

    // 적용 시작일
    @Column(nullable = false)
    private LocalDate effectiveFrom;

    // 적용 종료일 (null이면 현재 적용 중)
    private LocalDate effectiveTo;

    // 설명 (예: "2025년 최저임금")
    private String description;

    @CreationTimestamp
    private Timestamp createdAt;

    @UpdateTimestamp
    private Timestamp updatedAt;

    /**
     * 최저임금 업데이트
     */
    public void updateWageInfo(Integer hourlyWage, LocalDate effectiveFrom, LocalDate effectiveTo, String description) {
        this.hourlyWage = hourlyWage;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.description = description;
    }
}

