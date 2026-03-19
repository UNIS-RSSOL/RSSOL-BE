package com.rssolplan.api.domain.payroll;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 급여 지급 기록 Repository
 */
@Repository
public interface PaymentRecordRepository extends JpaRepository<PaymentRecord, Long> {

    /**
     * 특정 직원의 월별 지급 기록 조회
     *
     * @param userStoreId UserStore ID
     * @param year        년도
     * @param month       월
     * @return 지급 기록
     */
    Optional<PaymentRecord> findByUserStore_IdAndYearAndMonth(Long userStoreId, int year, int month);
}

