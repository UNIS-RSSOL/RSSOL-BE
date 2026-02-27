package com.example.unis_rssol.domain.payroll;

import com.example.unis_rssol.domain.payroll.dto.*;
import com.example.unis_rssol.global.security.annotation.AdminOnly;
import com.example.unis_rssol.global.security.annotation.OwnerOnly;
import jakarta.validation.Valid;
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

    // ==================== 매장 전체 인건비 합산 API ====================

    /**
     * OWNER: 매장 전체 인건비 합산 조회
     * - 매장의 모든 직원 급여 합산
     * - 직원별 급여 상세 목록 포함
     *
     * @param userId 현재 로그인 사용자 ID
     * @param year   조회 연도 (기본값: 현재 연도)
     * @param month  조회 월 (기본값: 현재 월)
     * @return 매장 전체 급여 요약 (StorePayrollSummaryDto)
     */
    @OwnerOnly
    @GetMapping("/store/total")
    public ResponseEntity<StorePayrollSummaryDto> getStorePayrollTotal(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month
    ) {
        LocalDate now = LocalDate.now();
        int targetYear = (year != null) ? year : now.getYear();
        int targetMonth = (month != null) ? month : now.getMonthValue();

        log.info("📊 [인건비합산] OWNER userId={} 매장 전체 인건비 조회 - {}/{}",
                userId, targetYear, targetMonth);

        StorePayrollSummaryDto summary = payrollService.getStorePayrollTotal(userId, targetYear, targetMonth);
        return ResponseEntity.ok(summary);
    }

    // ==================== 내가 속한 모든 매장의 급여 합산 API ====================

    /**
     * STAFF: 내가 속한 모든 매장의 급여 조회 (합산 포함)
     * - userId 기준 모든 매장 급여 합산
     * - 매장별 급여 상세 목록 포함
     *
     * @param userId 현재 로그인 사용자 ID
     * @param year   조회 연도 (기본값: 현재 연도)
     * @param month  조회 월 (기본값: 현재 월)
     * @return 내가 속한 모든 매장 급여 (MyPayrollListDto)
     */
    @GetMapping("/me/total")
    public ResponseEntity<MyPayrollListDto> getMyPayrollsTotal(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month
    ) {
        LocalDate now = LocalDate.now();
        int targetYear = (year != null) ? year : now.getYear();
        int targetMonth = (month != null) ? month : now.getMonthValue();

        log.info("📊 [내급여합산] userId={} 모든 매장 급여 조회 - {}/{}",
                userId, targetYear, targetMonth);

        MyPayrollListDto payrolls = payrollService.getMyPayrollsTotal(userId, targetYear, targetMonth);
        return ResponseEntity.ok(payrolls);
    }

    // ==================== 특정 직원 급여 상세 API (EmployeePayrollDto 형식) ====================

    /**
     * OWNER: 특정 직원의 급여 상세 조회 (상세 형식)
     *
     * @param userId      현재 로그인 사용자 ID
     * @param userStoreId 조회할 직원의 UserStore ID
     * @param year        조회 연도 (기본값: 현재 연도)
     * @param month       조회 월 (기본값: 현재 월)
     * @return 직원 급여 상세 (EmployeePayrollDto)
     */
    @OwnerOnly
    @GetMapping("/store/employee/{userStoreId}")
    public ResponseEntity<EmployeePayrollDto> getEmployeePayrollDetail(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long userStoreId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month
    ) {
        LocalDate now = LocalDate.now();
        int targetYear = (year != null) ? year : now.getYear();
        int targetMonth = (month != null) ? month : now.getMonthValue();

        log.info("📊 [직원급여상세] OWNER userId={} 직원 userStoreId={} 급여 상세 조회 - {}/{}",
                userId, userStoreId, targetYear, targetMonth);

        EmployeePayrollDto detail = payrollService.getEmployeePayrollDetail(userId, userStoreId, targetYear, targetMonth);
        return ResponseEntity.ok(detail);
    }

    // ==================== Admin: 최저임금 관리 API ====================

    /**
     * ADMIN: 최저임금 등록/수정
     * - 특정 관리자(rssolewha@gmail.com)만 접근 가능
     *
     * @param userId 현재 로그인 사용자 ID
     * @param dto    최저임금 정보
     * @return 등록된 최저임금 정보
     */
    @AdminOnly
    @PostMapping("/admin/minimum-wage")
    public ResponseEntity<MinimumWage> createMinimumWage(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody MinimumWageUpdateDto dto
    ) {
        log.info("⚙️ [Admin] userId={} 최저임금 등록 요청 - {}원 (적용: {} ~ {})",
                userId, dto.getHourlyWage(), dto.getEffectiveFrom(), dto.getEffectiveTo());

        MinimumWage created = payrollService.updateMinimumWage(dto);
        return ResponseEntity.ok(created);
    }

    /**
     * 최저임금 조회 (연도별)
     *
     * @param year 조회할 연도 (기본값: 현재 연도)
     * @return 해당 연도의 최저임금 정보
     */
    @GetMapping("/minimum-wage")
    public ResponseEntity<MinimumWage> getMinimumWage(
            @RequestParam(required = false) Integer year
    ) {
        int targetYear = (year != null) ? year : LocalDate.now().getYear();
        MinimumWage wage = payrollService.getMinimumWage(targetYear);
        return ResponseEntity.ok(wage);
    }

    /**
     * 현재 적용 중인 최저임금 조회
     *
     * @return 현재 적용 중인 최저임금 정보
     */
    @GetMapping("/minimum-wage/current")
    public ResponseEntity<MinimumWage> getCurrentMinimumWage() {
        MinimumWage wage = payrollService.getCurrentMinimumWage();
        return ResponseEntity.ok(wage);
    }

    // ==================== Owner: 직원 시급 관리 API ====================

    /**
     * OWNER: 매장 직원 시급 설정
     *
     * @param userId      현재 로그인 사용자 ID (OWNER)
     * @param userStoreId 시급을 설정할 직원의 UserStore ID
     * @param dto         시급 정보
     * @return 성공 응답
     */
    @OwnerOnly
    @PutMapping("/store/staff/{userStoreId}/wage")
    public ResponseEntity<Void> updateStaffWage(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long userStoreId,
            @Valid @RequestBody StaffWageUpdateDto dto
    ) {
        log.info("💰 [시급설정] OWNER userId={} 직원 userStoreId={} 시급 설정 - {}원",
                userId, userStoreId, dto.getHourlyWage());

        payrollService.updateStaffWage(userId, userStoreId, dto.getHourlyWage());
        return ResponseEntity.ok().build();
    }

    /**
     * OWNER: 매장 전체 직원 시급 목록 조회
     *
     * @param userId 현재 로그인 사용자 ID (OWNER)
     * @return 매장 직원들의 시급 목록
     */
    @OwnerOnly
    @GetMapping("/store/wages")
    public ResponseEntity<StoreStaffWagesResponseDto> getAllStaffWages(
            @AuthenticationPrincipal Long userId
    ) {
        log.info("📋 [시급조회] OWNER userId={} 매장 전체 직원 시급 조회", userId);

        StoreStaffWagesResponseDto wages = payrollService.getAllStaffWages(userId);
        return ResponseEntity.ok(wages);
    }
}

