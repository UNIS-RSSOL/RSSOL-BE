package com.example.unis_rssol.domain.store;

import com.example.unis_rssol.domain.payroll.PayrollService;
import com.example.unis_rssol.domain.store.dto.AllStaffSummaryResponseDto;
import com.example.unis_rssol.global.security.AuthorizationService;
import com.example.unis_rssol.global.security.annotation.OwnerOnly;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/store")
@RequiredArgsConstructor
public class StoreController {

    private final AuthorizationService authService;
    private final UserStoreRepository userStoreRepository;
    private final PayrollService payrollService;

    @GetMapping("/staff")
    public ResponseEntity<List<StoreStaffResponse>> getAllStaff(@AuthenticationPrincipal Long userId) {
        Long storeId = authService.getActiveStoreIdOrThrow(userId);

        // 2. 매장에 속한 모든 UserStore 가져오기
        List<UserStore> staffList = userStoreRepository.findByStore_Id(storeId);

        List<StoreStaffResponse> response = staffList.stream()
                .map(us -> new StoreStaffResponse(
                        us.getId(),               // userStoreId
                        us.getUser().getUsername() // username
                ))
                .toList();

        return ResponseEntity.ok(response);
    }

    /**
     * OWNER: 매장 전체 직원 요약 목록 조회 (직원관리용)
     * 이름, 역할, 이번달 급여, 연락처, 계좌, 지각/결근 횟수 포함
     *
     * @param userId 현재 로그인 사용자 ID
     * @param year   조회 연도 (기본값: 현재 연도)
     * @param month  조회 월 (기본값: 현재 월)
     * @return 매장 전체 직원 요약 목록
     */
    @OwnerOnly
    @GetMapping("/staff/summary")
    public ResponseEntity<AllStaffSummaryResponseDto> getAllStaffSummary(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month
    ) {
        LocalDate now = LocalDate.now();
        int targetYear = (year != null) ? year : now.getYear();
        int targetMonth = (month != null) ? month : now.getMonthValue();

        log.info("📋 [직원관리] OWNER userId={} 전체 직원 요약 목록 조회 - {}/{}",
                userId, targetYear, targetMonth);

        AllStaffSummaryResponseDto summary = payrollService.getAllStaffSummary(userId, targetYear, targetMonth);
        return ResponseEntity.ok(summary);
    }
}
