package com.rssolplan.api.global.security.email_verification;

import com.rssolplan.edu.global.security.email_verification.dto.EmailVerificationRequest;
import com.rssolplan.edu.global.security.email_verification.dto.EmailVerificationResponse;
import com.rssolplan.edu.global.security.email_verification.dto.EmailVerificationVerifyRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 교사용 이메일 인증 API Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/auth/email-verification")
@RequiredArgsConstructor
public class EmailVerificationController {

    private final EmailService emailService;

    /**
     * 인증 코드 발송 API
     * - 교직원 이메일(@korea.kr)만 인증 가능
     * - 6자리 코드를 이메일로 발송하고 5분 동안 유효
     *
     * @param request 이메일 정보
     * @return 발송 결과
     */
    @PostMapping("/send")
    public ResponseEntity<EmailVerificationResponse> sendVerificationCode(
            @Valid @RequestBody EmailVerificationRequest request) {
        try {
            log.info("[EmailVerification] 인증 코드 발송 요청: {}", request.getEmail());
            
            emailService.sendVerificationEmail(request.getEmail());
            
            return ResponseEntity.ok(
                    EmailVerificationResponse.builder()
                            .success(true)
                            .message("인증 코드가 이메일로 발송되었습니다. (5분간 유효)")
                            .build()
            );
        } catch (IllegalArgumentException e) {
            log.warn("[EmailVerification] 도메인 검증 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(EmailVerificationResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build()
                    );
        } catch (RuntimeException e) {
            log.error("[EmailVerification] 이메일 발송 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(EmailVerificationResponse.builder()
                            .success(false)
                            .message("이메일 발송 중 오류가 발생했습니다.")
                            .build()
                    );
        }
    }

    /**
     * 인증 코드 검증 API
     * - 사용자가 입력한 코드를 Redis에 저장된 코드와 비교
     * - 검증 성공 시 코드는 자동으로 삭제됨
     *
     * @param request 이메일과 인증 코드
     * @return 검증 결과
     */
    @PostMapping("/verify")
    public ResponseEntity<EmailVerificationResponse> verifyCode(
            @Valid @RequestBody EmailVerificationVerifyRequest request) {
        try {
            log.info("[EmailVerification] 인증 코드 검증 요청: {}", request.getEmail());
            
            boolean isValid = emailService.verifyCode(request.getEmail(), request.getCode());
            
            if (isValid) {
                log.info("[EmailVerification] 인증 성공: {}", request.getEmail());
                return ResponseEntity.ok(
                        EmailVerificationResponse.builder()
                                .success(true)
                                .message("이메일 인증이 완료되었습니다.")
                                .build()
                );
            } else {
                log.warn("[EmailVerification] 인증 코드 불일치 또는 만료: {}", request.getEmail());
                return ResponseEntity.badRequest()
                        .body(EmailVerificationResponse.builder()
                                .success(false)
                                .message("인증 코드가 일치하지 않거나 만료되었습니다.")
                                .build()
                        );
            }
        } catch (Exception e) {
            log.error("[EmailVerification] 인증 검증 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(EmailVerificationResponse.builder()
                            .success(false)
                            .message("인증 검증 중 오류가 발생했습니다.")
                            .build()
                    );
        }
    }
}

