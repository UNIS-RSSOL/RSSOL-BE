package com.rssolplan.api.global.security.email_verification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailVerificationCodeRepository
        extends JpaRepository<EmailVerificationCode, Long> {

    // 이메일로 가장 최근 인증 코드 조회
    Optional<EmailVerificationCode> findTopByEmailOrderByCreatedAtDesc(String email);

    // 이메일과 코드로 조회
    Optional<EmailVerificationCode> findByEmailAndCode(String email, String code);

    // 이메일로 모든 인증 코드 삭제
    void deleteByEmail(String email);
}
