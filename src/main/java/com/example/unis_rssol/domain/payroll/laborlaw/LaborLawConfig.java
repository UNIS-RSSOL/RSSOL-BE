package com.example.unis_rssol.domain.payroll.laborlaw;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@RequiredArgsConstructor
@Entity
@Builder
@Table(name = "labor_law_config")
@AllArgsConstructor
public class LaborLawConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String configKey;      // "MINIMUM_WAGE_2025"
    private String configValue;    // "10030"
    private LocalDate effectiveFrom;  // 적용 시작일
    private LocalDate effectiveTo;    // 적용 종료일

    private LocalDateTime updatedAt;
    private String updatedBy;
}

