package com.example.unis_rssol.domain.payroll;

import com.example.unis_rssol.domain.payroll.dto.OwnerPayrollSummaryDto;
import com.example.unis_rssol.domain.payroll.dto.StaffMyPayrollResponseDto;
import com.example.unis_rssol.domain.payroll.dto.StaffPayrollResponseDto;
import com.example.unis_rssol.global.security.annotation.OwnerOnly;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * [생성 이유]
 * 급여 조회 API를 제공하는 Controller.
 * OWNER와 STAFF 권한에 따라 다른 조회 기능 제공.
 * <p>
 * [역할]
 * - OWNER: 매장 전체 급여 현황 조회, 개별 직원 급여 조회
 * - STAFF: 본인의 모든 매장 급여 조회
 */
@Slf4j
@RestController
@RequestMapping("/api/payroll")
@RequiredArgsConstructor
public class PayrollController {

    private final PayrollService payrollService;

    /**
     * OWNER: 활성 매장의 전체 직원 급여 현황 조회
     *
     * @param userId 현재 로그인 사용자 ID
     * @param year   조회 연도 (기본값: 현재 연도)
     * @param month  조회 월 (기본값: 현재 월)
     * @return 매장 급여 요약 (총합계 + 직원별 상세)
     */
    @OwnerOnly
    @GetMapping("/store/summary")
    public ResponseEntity<OwnerPayrollSummaryDto> getStorePayrollSummary(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month
    ) {
        LocalDate now = LocalDate.now();
        int targetYear = (year != null) ? year : now.getYear();
        int targetMonth = (month != null) ? month : now.getMonthValue();

        log.info("📊 [급여조회] OWNER userId={} 매장 급여 현황 조회 - {}/{}",
                userId, targetYear, targetMonth);

        OwnerPayrollSummaryDto summary = payrollService.getStorePayrollSummary(userId, targetYear, targetMonth);
        return ResponseEntity.ok(summary);
    }

    /**
     * OWNER: 특정 직원의 급여 상세 조회
     *
     * @param userId      현재 로그인 사용자 ID
     * @param userStoreId 조회할 직원의 UserStore ID
     * @param year        조회 연도 (기본값: 현재 연도)
     * @param month       조회 월 (기본값: 현재 월)
     * @return 직원 급여 상세
     */
    @OwnerOnly
    @GetMapping("/store/staff/{userStoreId}")
    public ResponseEntity<StaffPayrollResponseDto> getStaffPayrollDetail(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long userStoreId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month
    ) {
        LocalDate now = LocalDate.now();
        int targetYear = (year != null) ? year : now.getYear();
        int targetMonth = (month != null) ? month : now.getMonthValue();

        log.info("📊 [급여조회] OWNER userId={} 직원 userStoreId={} 급여 상세 조회 - {}/{}",
                userId, userStoreId, targetYear, targetMonth);

        StaffPayrollResponseDto detail = payrollService.getStaffPayrollDetail(userId, userStoreId, targetYear, targetMonth);
        return ResponseEntity.ok(detail);
    }

    /**
     * STAFF: 본인이 근무하는 모든 매장의 급여 조회
     *
     * @param userId 현재 로그인 사용자 ID
     * @param year   조회 연도 (기본값: 현재 연도)
     * @param month  조회 월 (기본값: 현재 월)
     * @return 매장별 급여 목록
     */
    @GetMapping("/me")
    public ResponseEntity<List<StaffMyPayrollResponseDto>> getMyPayrolls(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month
    ) {
        LocalDate now = LocalDate.now();
        int targetYear = (year != null) ? year : now.getYear();
        int targetMonth = (month != null) ? month : now.getMonthValue();

        log.info("📊 [급여조회] STAFF userId={} 본인 급여 조회 - {}/{}",
                userId, targetYear, targetMonth);

        List<StaffMyPayrollResponseDto> payrolls = payrollService.getMyPayrolls(userId, targetYear, targetMonth);
        return ResponseEntity.ok(payrolls);
    }
}

