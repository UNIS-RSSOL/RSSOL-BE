package com.example.unis_rssol.domain.payroll;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface MinimumWageRepository extends JpaRepository<MinimumWage, Long> {

    /**
     * 특정 날짜에 적용되는 최저임금 조회
     */
    @Query("SELECT m FROM MinimumWage m " +
            "WHERE m.effectiveFrom <= :date " +
            "AND (m.effectiveTo IS NULL OR m.effectiveTo >= :date) " +
            "ORDER BY m.effectiveFrom DESC")
    Optional<MinimumWage> findByEffectiveDate(@Param("date") LocalDate date);

    /**
     * 현재 적용 중인 최저임금 조회
     */
    @Query("SELECT m FROM MinimumWage m " +
            "WHERE m.effectiveTo IS NULL " +
            "ORDER BY m.effectiveFrom DESC")
    Optional<MinimumWage> findCurrentMinimumWage();

    /**
     * 특정 연도에 적용되는 최저임금 조회
     */
    @Query("SELECT m FROM MinimumWage m " +
            "WHERE YEAR(m.effectiveFrom) = :year " +
            "ORDER BY m.effectiveFrom DESC")
    Optional<MinimumWage> findByYear(@Param("year") int year);
}

