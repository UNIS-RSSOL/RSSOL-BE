package com.example.unis_rssol.schedule.workavailability;

import com.example.unis_rssol.global.AuthorizationService;
import com.example.unis_rssol.global.exception.ForbiddenException;
import com.example.unis_rssol.global.exception.InvalidTimeRangeException;
import com.example.unis_rssol.global.exception.NotFoundException;
import com.example.unis_rssol.schedule.workavailability.dto.*;
import com.example.unis_rssol.store.entity.UserStore;
import com.example.unis_rssol.store.repository.UserStoreRepository;
import com.example.unis_rssol.user.entity.AppUser;
import com.example.unis_rssol.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkAvailabilityService {

    private final WorkAvailabilityRepository availabilityRepository;
    private final AuthorizationService authService;

    @Transactional(readOnly = true)
    public WorkAvailabilityGetResponseDto getAvailability(Long userId) {
        Long storeId = authService.getActiveStoreIdOrThrow(userId);
        UserStore requester = authService.getUserStoreOrThrow(userId, storeId);
        List<WorkAvailability> availabilities = availabilityRepository.findByUserStore(requester);
        List<WorkAvailabilityGetResponseDto.AvailabilityItem> items = availabilities.stream()
                .map(a -> new WorkAvailabilityGetResponseDto.AvailabilityItem(
                        a.getId(),
                        a.getDayOfWeek(),
                        a.getStartTime().toString(),
                        a.getEndTime().toString()
                ))
                .toList();

        return WorkAvailabilityGetResponseDto.builder()
                .message("근무 가능 시간 조회에 성공했습니다.")
                .userStoreId(requester.getId())
                .availabilities(items)
                .build();
    }

    @Transactional(readOnly = true)
    public List<WorkAvailabilityAllResponseDto> getAllAvailability(Long userId,Long storeId) {
        UserStore requester = authService.getUserStoreOrThrow(userId, storeId);
        if (requester.getPosition() != UserStore.Position.OWNER) {throw new ForbiddenException("해당 매장을 조회할 권한이 없습니다."); }

        List<WorkAvailability> allAvailabilities = availabilityRepository.findByUserStore_Store_Id(storeId);
        Map<Long, List<WorkAvailability>> groupedByUser = new HashMap<>();
        for (WorkAvailability avail : allAvailabilities) {
            Long userStoreId = avail.getUserStore().getId(); if (!groupedByUser.containsKey(userStoreId)) { groupedByUser.put(userStoreId, new ArrayList<>());}
            groupedByUser.get(userStoreId).add(avail);
        }

        List<WorkAvailabilityAllResponseDto> responseList = new ArrayList<>();

        for (Map.Entry<Long, List<WorkAvailability>> entry : groupedByUser.entrySet()) {
            WorkAvailabilityAllResponseDto userAvailabilityDto = getWorkAvailabilityAllResponseDto(entry);

            responseList.add(userAvailabilityDto);
        }

        return responseList;
    }


    @Transactional //Transactional :
    public WorkAvailabilityCreateResponseDto createAvailabilities(Long userId, WorkAvailabilityRequestDto request) {
        //유저 조회및 권한 체크!
        Long storeId = authService.getActiveStoreIdOrThrow(userId);
        UserStore requester = authService.getUserStoreOrThrow(userId, storeId);
        int insertedCount = 0;

        for (WorkAvailabilityRequestDto.AvailabilityItem item : request.getAvailabilities()) {
            LocalTime start = parseTime(item.getStartTime());
            LocalTime end = parseTime(item.getEndTime());
            validateTimeRange(start, end);

            // 2. 중복 체크
            boolean exists = availabilityRepository
                    .findByUserStoreAndDayOfWeekAndStartTimeAndEndTime(requester, item.getDayOfWeek(), start, end)
                    .isPresent();

            if (!exists) {
                saveAvailability(requester, item.getDayOfWeek(), start, end);
                insertedCount++;
            }
        }
        //3. Response
        return WorkAvailabilityCreateResponseDto.builder()
                .message("근무 가능 시간이 등록되었습니다.")
                .userStoreId(requester.getId())
                .inserted(insertedCount)
                .build();
    }


    @Transactional
    public List<WorkAvailabilityPatchResponseDto> replaceAvailabilities(Long userId, WorkAvailabilityRequestDto request) {
        // 1. userStore 조회 -> DB에 있는 availability조회 -> 요청데이터와 비교!
        Long storeId = authService.getActiveStoreIdOrThrow(userId);
        UserStore userStore = authService.getUserStoreOrThrow(userId, storeId);
        List<WorkAvailability> existing = availabilityRepository.findByUserStore(userStore);

        Map<WorkAvailability.DayOfWeek, WorkAvailabilityRequestDto.AvailabilityItem> requestMap =
                request.getAvailabilities().stream()
                        .collect(Collectors.toMap(
                                WorkAvailabilityRequestDto.AvailabilityItem::getDayOfWeek,
                                item -> item,
                                (oldItem, newItem) -> newItem // 중복이면 나중 값으로 덮어쓰기
                        ));

        List<WorkAvailabilityPatchResponseDto> results = new ArrayList<>();
        // 기존 데이터 삭제/업데이트
        handleExistingAvailabilities(existing, requestMap, results);
        // 남은 요청은 신규 등록
        handleNewAvailabilities(userStore, requestMap, results);

        return results;
    }
    // ----------------- Private Helpers -----------------

    private void handleExistingAvailabilities(List<WorkAvailability> existing,
                                              Map<WorkAvailability.DayOfWeek, WorkAvailabilityRequestDto.AvailabilityItem> requestMap,
                                              List<WorkAvailabilityPatchResponseDto> results) {

        for (WorkAvailability avail : existing) {
            WorkAvailabilityRequestDto.AvailabilityItem item = requestMap.get(avail.getDayOfWeek());

            if (item == null) {
                availabilityRepository.delete(avail);
                results.add(status(avail.getId(), AvailabilityStatus.DELETED));
                continue;
            }

            LocalTime start = parseTime(item.getStartTime());
            LocalTime end = parseTime(item.getEndTime());
            validateTimeRange(start, end);

            if (!avail.getStartTime().equals(start) || !avail.getEndTime().equals(end)) {
                avail.setStartTime(start);
                avail.setEndTime(end);
//               availabilityRepository.save(avail); 기존에걸 수정만하므로 JPA가 알아서 flush. 저장필요X
                results.add(status(avail.getId(), AvailabilityStatus.UPDATED));
            }

            requestMap.remove(avail.getDayOfWeek());
        }
    }


    private void handleNewAvailabilities(UserStore userStore,
                                         Map<WorkAvailability.DayOfWeek, WorkAvailabilityRequestDto.AvailabilityItem> requestMap,
                                         List<WorkAvailabilityPatchResponseDto> results) {

        for (WorkAvailabilityRequestDto.AvailabilityItem item : requestMap.values()) {
            LocalTime start = parseTime(item.getStartTime());
            LocalTime end = parseTime(item.getEndTime());
            validateTimeRange(start, end);

            WorkAvailability newAvail = saveAvailability(userStore, item.getDayOfWeek(), start, end);
            results.add(status(newAvail.getId(), AvailabilityStatus.INSERTED));
        }
    }


    private WorkAvailability saveAvailability(UserStore userStore,
                                              WorkAvailability.DayOfWeek dayOfWeek,
                                              LocalTime start,
                                              LocalTime end) {
        WorkAvailability avail = WorkAvailability.builder()
                .userStore(userStore)
                .dayOfWeek(dayOfWeek)
                .startTime(start)
                .endTime(end)
                .build();
        return availabilityRepository.save(avail);
    }

    private WorkAvailabilityPatchResponseDto status(Long id, AvailabilityStatus status) {
        return WorkAvailabilityPatchResponseDto.builder()
                .availabilityId(id)
                .status(status)
                .build();
    }

    // --- helper methods --- 시간형식검사 error 처리
    private LocalTime parseTime(String timeStr) {
        try {
            return LocalTime.parse(timeStr);
        } catch (Exception e) {
            throw new InvalidTimeRangeException("시간 형식이 잘못되었습니다. 예: 09:00:00");
        }
    }


    private void validateTimeRange(LocalTime start, LocalTime end) {
        if (!start.isBefore(end)) {
            throw new InvalidTimeRangeException("start_time must be earlier than end_time");
        }
    }

    private static WorkAvailabilityAllResponseDto getWorkAvailabilityAllResponseDto(Map.Entry<Long, List<WorkAvailability>> entry) {
        Long userStoreId = entry.getKey();
        List<WorkAvailability> availabilities = entry.getValue();

        String userName = availabilities.get(0).getUserStore().getUser().getUsername(); //모두 같은 UserStore이니까, 첫번째에서만 추출함.

        List<WorkAvailabilityAllResponseDto.AvailabilityItem> availabilityItems = new ArrayList<>();
        for (WorkAvailability a : availabilities) {
            WorkAvailabilityAllResponseDto.AvailabilityItem item =
                    new WorkAvailabilityAllResponseDto.AvailabilityItem(
                            a.getDayOfWeek(),
                            a.getStartTime().toString(),
                            a.getEndTime().toString()
                    );
            availabilityItems.add(item);
        }

        return new WorkAvailabilityAllResponseDto(userStoreId, userName, availabilityItems);
    }


}
