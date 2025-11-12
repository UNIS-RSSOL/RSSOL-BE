package com.example.unis_rssol.schedule.workshifts;

import com.example.unis_rssol.global.auth.AuthorizationService;
import com.example.unis_rssol.global.exception.NotFoundException;
import com.example.unis_rssol.schedule.entity.WorkShift;
import com.example.unis_rssol.schedule.repository.WorkShiftRepository;
import com.example.unis_rssol.schedule.workshifts.dto.WorkShiftCreateDto;
import com.example.unis_rssol.schedule.workshifts.dto.WorkShiftDto;
import com.example.unis_rssol.schedule.workshifts.dto.WorkShiftUpdateDto;
import com.example.unis_rssol.store.entity.Store;
import com.example.unis_rssol.store.entity.UserStore;
import com.example.unis_rssol.store.repository.StoreRepository;
import com.example.unis_rssol.store.repository.UserStoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkShiftService {
    private final WorkShiftRepository workShiftRepository;
    private final StoreRepository storeRepository;
    private final AuthorizationService authService;
    private final UserStoreRepository userStoreRepository;

    @Transactional(readOnly = true)
    public List<WorkShiftDto> getWorkShifts(Long userId) {
        // 사용자의 활성 매장 조회
        Long storeId = authService.getActiveStoreIdOrThrow(userId);
        // 엔티티 조회
        List<WorkShift> workShifts = workShiftRepository.findByStore_Id(storeId);
        List<WorkShiftDto> dtos = new ArrayList<>();

        for (WorkShift shift : workShifts) {
            WorkShiftDto dto = new WorkShiftDto(shift);
            dtos.add(dto);
        }
        // DTO 변환
        return dtos;
    }

    @Transactional(readOnly = true)
    public List<WorkShift> getWorkShiftsByPeriod(Long userId, LocalDate startDate, LocalDate endDate) {
        Long storeId = authService.getActiveStoreIdOrThrow(userId);

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);
        System.out.println("조회 범위: " + start + " ~ " + end);
        return workShiftRepository.findByStoreIdAndDateRange(storeId, start, end);
    }

    /** 🟢 근무블록 추가 **/

    @Transactional
    public WorkShiftDto createWorkShift(Long userId, WorkShiftCreateDto dto) {
        Long storeId = authService.getActiveStoreIdOrThrow(userId);
        log.debug("✅ storeId={}, userStoreId={}", storeId, dto.getUserStoreId());
        Store store = storeRepository.findById(storeId).orElseThrow(() -> new NotFoundException("해당 매장이 존재하지 않습니다."));
        UserStore userStore = userStoreRepository.findById(dto.getUserStoreId())
                .orElseThrow(() -> new NotFoundException("UserStore not found"));

        WorkShift workShift = new WorkShift();
        workShift.setStore(store);
        workShift.setUserStore(userStore);
        workShift.setStartDatetime(dto.getStartDatetime());
        workShift.setEndDatetime(dto.getEndDatetime());
        workShift.setShiftStatus(WorkShift.ShiftStatus.SCHEDULED);

        return new WorkShiftDto(workShiftRepository.save(workShift));
    }

    /** 🟡 근무블록 수정 **/
    @Transactional
    public WorkShiftDto updateWorkShift(Long userId, Long shiftId, WorkShiftUpdateDto dto) {
        Long storeId = authService.getActiveStoreIdOrThrow(userId);

        WorkShift workShift = workShiftRepository.findById(shiftId)
                .orElseThrow(() -> new IllegalArgumentException("WorkShift not found"));

        if (!workShift.getStore().getId().equals(storeId)) {
            throw new SecurityException("해당 매장 소속이 아닙니다.");
        }

        workShift.setStartDatetime(dto.getStartDatetime());
        workShift.setEndDatetime(dto.getEndDatetime());

        return new WorkShiftDto(workShiftRepository.save(workShift));
    }

    /** 🔴 근무블록 삭제 **/
    @Transactional
    public void deleteWorkShift(Long userId, Long shiftId) {
        Long storeId = authService.getActiveStoreIdOrThrow(userId);

        WorkShift workShift = workShiftRepository.findById(shiftId)
                .orElseThrow(() -> new IllegalArgumentException("WorkShift not found"));

        if (!workShift.getStore().getId().equals(storeId)) {
            throw new SecurityException("해당 매장 소속이 아닙니다.");
        }

        workShiftRepository.delete(workShift);
    }
}
