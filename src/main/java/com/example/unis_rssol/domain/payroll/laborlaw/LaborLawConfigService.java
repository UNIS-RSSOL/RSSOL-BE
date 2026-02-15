package com.example.unis_rssol.domain.payroll.laborlaw;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class LaborLawConfigService {

    private final LaborLawConfigRepository repository;

    public int getMinimumWage(LocalDate targetDate) {
        return repository.findByKeyAndDate("MINIMUM_WAGE", targetDate)
                .map(config -> Integer.parseInt(config.getConfigValue()))
                .orElseThrow(() -> new IllegalStateException("최저임금 설정 없음"));
    }

    @Transactional
    public void updateMinimumWage(int newWage, LocalDate from) {

        // 1. 현재 적용 중인 최저임금 종료 처리
        repository.findActiveConfig("MINIMUM_WAGE")
                .ifPresent(config ->
                        config.setEffectiveTo(from.minusDays(1))
                );

        // 2. 새 최저임금 설정 추가
        LaborLawConfig newConfig = LaborLawConfig.builder()
                .configKey("MINIMUM_WAGE")
                .configValue(String.valueOf(newWage))
                .effectiveFrom(from)
                .effectiveTo(LocalDate.of(9999, 12, 31))
                .updatedAt(LocalDateTime.now())
                .updatedBy("ADMIN")
                .build();

        repository.save(newConfig);
    }
}

