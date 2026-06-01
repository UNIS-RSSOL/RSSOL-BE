package com.rssolplan.api.domain.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 이메일 인증 이력 Repository
 */
@Repository
public interface EmailVerificationHistoryRepository extends JpaRepository<EmailVerificationHistory, Long> {

    /**
     * 이메일별 최근 인증 기록 조회
     */
    Optional<EmailVerificationHistory> findFirstByEmailOrderByCreatedAtDesc(String email);

    /**
     * 이메일별 모든 인증 기록 조회
     */
    List<EmailVerificationHistory> findByEmailOrderByCreatedAtDesc(String email);

    /**
     * 특정 기간 동안의 인증 기록 조회
     */
    @Query("SELECT e FROM EmailVerificationHistory e WHERE e.createdAt BETWEEN :startDate AND :endDate ORDER BY e.createdAt DESC")
    List<EmailVerificationHistory> findByDateRange(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * 상태별 인증 기록 조회
     */
    List<EmailVerificationHistory> findByStatusOrderByCreatedAtDesc(EmailVerificationHistory.VerificationStatus status);

    /**
     * 이메일과 상태로 기록 조회
     */
    List<EmailVerificationHistory> findByEmailAndStatusOrderByCreatedAtDesc(String email, EmailVerificationHistory.VerificationStatus status);
}

