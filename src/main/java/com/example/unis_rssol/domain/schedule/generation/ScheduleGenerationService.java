package com.example.unis_rssol.domain.schedule.generation;

import com.example.unis_rssol.domain.notification.NotificationService;
import com.example.unis_rssol.domain.schedule.DayOfWeek;
import com.example.unis_rssol.domain.schedule.generation.dto.*;
import com.example.unis_rssol.domain.schedule.generation.dto.candidate.CandidateSchedule;
import com.example.unis_rssol.domain.schedule.generation.dto.candidate.CandidateShift;
import com.example.unis_rssol.domain.schedule.generation.dto.candidate.GenerationOptionsDto;
import com.example.unis_rssol.domain.schedule.generation.dto.setting.ScheduleSettingSegmentResponseDto;
import com.example.unis_rssol.domain.schedule.generation.entity.Schedule;
import com.example.unis_rssol.domain.schedule.generation.entity.ScheduleRequest;
import com.example.unis_rssol.domain.schedule.generation.entity.ScheduleRequest.ScheduleRequestStatus;
import com.example.unis_rssol.domain.schedule.generation.entity.WorkShift;
import com.example.unis_rssol.domain.schedule.generation.strategy.*;
import com.example.unis_rssol.domain.schedule.workavailability.WorkAvailability;
import com.example.unis_rssol.domain.schedule.workavailability.WorkAvailabilityRepository;
import com.example.unis_rssol.domain.schedule.workshifts.WorkShiftRepository;
import com.example.unis_rssol.domain.store.Store;
import com.example.unis_rssol.domain.store.StoreRepository;
import com.example.unis_rssol.domain.store.UserStore;
import com.example.unis_rssol.domain.store.UserStoreRepository;
import com.example.unis_rssol.domain.store.setting.StoreSetting;
import com.example.unis_rssol.domain.store.setting.StoreSettingRepository;
import com.example.unis_rssol.domain.store.setting.StoreSettingSegment;
import com.example.unis_rssol.global.exception.ForbiddenException;
import com.example.unis_rssol.global.exception.NotFoundException;
import com.example.unis_rssol.global.security.AuthorizationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.example.unis_rssol.domain.store.UserStore.Position.OWNER;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleGenerationService {
    private final StoreRepository storeRepository;
    private final AuthorizationService authService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final WorkAvailabilityRepository workAvailabilityRepository;
    private final UserStoreRepository userStoreRepository;
    private final ScheduleRepository scheduleRepository;
    private final WorkShiftRepository workShiftRepository;
    private final NotificationService notificationService;
    private final StoreSettingRepository storeSettingRepository;
    private final ScheduleRequestRepository scheduleRequestRepository;

    // 전략 패턴 - 4가지 전략 주입
    private final BalancedStrategy balancedStrategy;
    private final CoverageFirstStrategy coverageFirstStrategy;
    private final SeniorPriorityStrategy seniorPriorityStrategy;
    private final FairDistributionStrategy fairDistributionStrategy;

    private ObjectMapper objectMapper;

    @jakarta.annotation.PostConstruct
    public void init() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    // ========================================
    // 1. 스케줄 요청 (알바생에게 근무 가능 시간 입력 요청)
    // ========================================
    @Transactional
    public ScheduleRequestResponseDto requestSchedule(Long userId, ScheduleRequestDto request) {
        Long storeId = authService.getActiveStoreIdOrThrow(userId);
        UserStore owner = authService.getUserStoreOrThrow(userId, storeId);
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 매장입니다."));

        if (owner.getPosition() != OWNER) {
            throw new ForbiddenException("해당 매장의 근무표를 생성할 권한이 없습니다.");
        }

        // 매장 기본 설정 검증 (시간대 정보 필수)
        storeSettingRepository.findByStoreId(storeId)
                .orElseThrow(() -> new NotFoundException("매장 기본 설정이 존재하지 않습니다. 온보딩을 완료해주세요."));

        // 인원수 정보를 Redis에 임시 저장
        String staffRequirementKey = null;
        if (request.getStaffRequirement() != null) {
            staffRequirementKey = saveStaffRequirementToRedis(storeId, request.getStaffRequirement());
        }

        // ScheduleRequest 생성
        ScheduleRequest scheduleRequest = ScheduleRequest.builder()
                .store(store)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(ScheduleRequestStatus.REQUESTED)
                .temporarySettingKey(staffRequirementKey) // 인원수 정보 저장 키
                .build();

        scheduleRequestRepository.save(scheduleRequest);

        // 알림 생성
        notificationService.sendScheduleInputRequest(userId, storeId,
                request.getStartDate(), request.getEndDate());

        return ScheduleRequestResponseDto.builder()
                .scheduleRequestId(scheduleRequest.getId())
                .storeId(storeId)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(scheduleRequest.getStatus().name())
                .build();
    }

    /**
     * 인원수 설정을 Redis에 저장
     */
    private String saveStaffRequirementToRedis(Long storeId, StaffRequirementDto dto) {
        String key = "staff_requirement:store:" + storeId + ":" + UUID.randomUUID();
        try {
            String json = objectMapper.writeValueAsString(dto);
            redisTemplate.opsForValue().set(key, json, Duration.ofDays(7));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("인원수 설정 저장 실패", e);
        }
        return key;
    }

    // ========================================
    // 3. 스케줄 생성 (후보군 생성)
    // ========================================
    @Transactional
    public ScheduleGenerationResponseDto generateSchedule(Long userId, Long scheduleRequestId,
                                                          ScheduleGenerationRequestDto request) {
        Long storeId = authService.getActiveStoreIdOrThrow(userId);

        ScheduleRequest scheduleRequest = scheduleRequestRepository.findById(scheduleRequestId)
                .orElseThrow(() -> new NotFoundException("스케줄 요청을 찾을 수 없습니다."));

        if (scheduleRequest.getStatus() != ScheduleRequestStatus.REQUESTED) {
            throw new IllegalStateException("아직 요청 상태가 아닙니다.");
        }

        // 근무 가능 시간 모두 제출됐는지 확인
        List<Long> unsubmitted = validateAllSubmitted(storeId);
        if (!unsubmitted.isEmpty()) {
            throw new IllegalStateException("아직 근무 시간표를 제출하지 않은 직원이 있습니다: " + unsubmitted);
        }

        // 설정 조회 (StoreSetting 또는 Redis 임시 설정)
        ScheduleSettingSnapshot settingSnapshot = getSettingSnapshot(scheduleRequest);

        // 전략 기반 후보 스케줄 생성
        GenerationOptionsDto options = request.getGenerationOptions();
        List<CandidateSchedule> candidates = generateCandidatesWithStrategies(storeId, settingSnapshot, options);

        // Redis에 후보 저장
        String redisKey = saveCandidateSchedulesToRedis(storeId, candidates);

        // ScheduleRequest 상태 업데이트
        scheduleRequest.setStatus(ScheduleRequestStatus.GENERATED);
        scheduleRequest.setCandidateScheduleKey(redisKey);
        scheduleRequestRepository.save(scheduleRequest);

        return buildResponse(scheduleRequest, storeId, settingSnapshot.getSegments(),
                redisKey, candidates.size());
    }

    // ========================================
    // 4. 후보 스케줄 조회
    // ========================================
    public List<CandidateSchedule> getCandidateSchedules(String redisKey) {
        String jsonFromRedis = (String) redisTemplate.opsForValue().get(redisKey);
        if (jsonFromRedis == null) {
            throw new NotFoundException("생성된 근무표가 없습니다.");
        }

        try {
            return objectMapper.readValue(jsonFromRedis, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Redis 캐시 읽기 실패", e);
        }
    }

    // ========================================
    // 5. 스케줄 확정
    // ========================================
    @Transactional
    public Schedule finalizeCandidateSchedule(Long userId, Long scheduleRequestId, int candidateIndex) {
        Long storeId = authService.getActiveStoreIdOrThrow(userId);

        ScheduleRequest scheduleRequest = scheduleRequestRepository.findById(scheduleRequestId)
                .orElseThrow(() -> new NotFoundException("스케줄 요청을 찾을 수 없습니다."));

        if (scheduleRequest.getStatus() != ScheduleRequestStatus.GENERATED) {
            throw new IllegalStateException("아직 후보 스케줄이 생성되지 않았습니다.");
        }

        LocalDate startDate = scheduleRequest.getStartDate();
        LocalDate endDate = scheduleRequest.getEndDate();
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        // 기존 근무블록 삭제
        workShiftRepository.deleteOverlappingShifts(storeId, start, end);

        // Schedule 엔티티 생성
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new NotFoundException("매장을 찾을 수 없습니다."));
        Schedule schedule = new Schedule();
        schedule.setStore(store);
        schedule.setStartDate(startDate);
        schedule.setEndDate(endDate);

        // Redis에서 CandidateSchedule 가져오기
        List<CandidateSchedule> candidates = getCandidateSchedules(scheduleRequest.getCandidateScheduleKey());

        if (candidates.isEmpty() || candidateIndex >= candidates.size()) {
            throw new IllegalStateException("유효하지 않은 후보 인덱스입니다.");
        }

        CandidateSchedule selected = candidates.get(candidateIndex);

        // CandidateShift → WorkShift 변환
        for (CandidateShift shift : selected.getShifts()) {
            if (shift.getUserStoreId() == null) continue; // UNASSIGNED

            WorkShift ws = new WorkShift();
            ws.setShiftStatus(WorkShift.ShiftStatus.SCHEDULED);
            ws.setUserStore(userStoreRepository.findById(shift.getUserStoreId())
                    .orElseThrow(() -> new NotFoundException("직원 정보를 찾을 수 없습니다.")));
            ws.setStore(store);
            ws.setSchedule(schedule);
            schedule.getWorkShifts().add(ws);

            LocalDate shiftDate = startDate.plusDays(shift.getDay().getValue() - 1);
            ws.setStartDatetime(shiftDate.atTime(shift.getStartTime()));
            ws.setEndDatetime(shiftDate.atTime(shift.getEndTime()));
        }

        // Schedule 저장
        Schedule saved = scheduleRepository.save(schedule);

        // ScheduleRequest 상태 업데이트
        scheduleRequest.setStatus(ScheduleRequestStatus.CONFIRMED);
        scheduleRequest.setSchedule(saved);
        scheduleRequestRepository.save(scheduleRequest);

        // Redis 정리
        redisTemplate.delete(scheduleRequest.getCandidateScheduleKey());
        if (scheduleRequest.getTemporarySettingKey() != null) {
            redisTemplate.delete(scheduleRequest.getTemporarySettingKey());
        }

        return saved;
    }

    // ========================================
    // 내부 헬퍼 메서드
    // ========================================

    /**
     * 설정 스냅샷 조회
     * - StoreSetting에서 시간대 정보 조회
     * - Redis에서 인원수 정보 조회하여 결합
     */
    private ScheduleSettingSnapshot getSettingSnapshot(ScheduleRequest request) {
        Long storeId = request.getStore().getId();

        // StoreSetting에서 시간대 정보 조회
        StoreSetting storeSetting = storeSettingRepository.findByStoreId(storeId)
                .orElseThrow(() -> new NotFoundException("기본 매장 설정이 존재하지 않습니다."));

        // Redis에서 인원수 정보 조회
        StaffRequirementDto staffRequirement = null;
        if (request.getTemporarySettingKey() != null) {
            staffRequirement = getStaffRequirementFromRedis(request.getTemporarySettingKey());
        }

        return ScheduleSettingSnapshot.fromStoreSettingWithStaff(storeSetting, staffRequirement);
    }

    /**
     * Redis에서 인원수 설정 조회
     */
    private StaffRequirementDto getStaffRequirementFromRedis(String key) {
        String json = (String) redisTemplate.opsForValue().get(key);
        if (json == null) {
            return null; // 인원수 정보 없으면 기본값 사용
        }

        try {
            return objectMapper.readValue(json, StaffRequirementDto.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("인원수 설정 읽기 실패", e);
        }
    }

    /**
     * 전략 패턴 기반 후보 스케줄 생성
     * - 각 전략별로 1개의 후보 스케줄 생성
     * - 기본: 4가지 전략 모두 사용 (BALANCED, COVERAGE_FIRST, SENIOR_PRIORITY, FAIR_DISTRIBUTION)
     */
    public List<CandidateSchedule> generateCandidatesWithStrategies(Long storeId,
                                                                    ScheduleSettingSnapshot settings,
                                                                    GenerationOptionsDto options) {
        // 근무 가능자 로드
        List<WorkAvailability> availabilities = workAvailabilityRepository.findByUserStore_Store_Id(storeId);
        if (availabilities.isEmpty()) {
            throw new IllegalStateException("근무 가능 시간을 제출한 직원이 없습니다.");
        }

        // 직원 username 매핑
        Map<Long, String> userStoreUsernameMap = userStoreRepository
                .findUserStoreIdAndUsernameByStoreId(storeId)
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (String) row[1]
                ));

        // 직원 경력(hireDate) 매핑
        Map<Long, LocalDate> userStoreHireDateMap = userStoreRepository.findByStore_Id(storeId)
                .stream()
                .collect(Collectors.toMap(
                        UserStore::getId,
                        us -> us.getHireDate() != null ? us.getHireDate() : LocalDate.now()
                ));

        // 사용할 전략 결정
        List<ScheduleGenerationStrategy> strategiesToUse = getStrategiesToUse(options);

        List<CandidateSchedule> candidateSchedules = new ArrayList<>();

        for (ScheduleGenerationStrategy strategy : strategiesToUse) {
            log.info("🔄 전략 '{}' 으로 후보 스케줄 생성 중...", strategy.getStrategyName());

            CandidateSchedule candidate = strategy.generate(
                    storeId,
                    settings,
                    availabilities,
                    userStoreUsernameMap,
                    userStoreHireDateMap
            );

            // 전략 정보 설정
            candidate.setStrategyName(strategy.getStrategyName());
            candidate.setStrategyDescription(strategy.getDescription());

            // 메타데이터 계산 (배정률 등)
            candidate.calculateMetadata();

            log.info("✅ 전략 '{}' 완료 - 배정률: {}%, 빈자리: {}개",
                    strategy.getStrategyName(),
                    candidate.getCoverageRate(),
                    candidate.getUnassignedCount());

            candidateSchedules.add(candidate);
        }

        return candidateSchedules;
    }

    /**
     * 사용할 전략 목록 결정
     * 1. 항상 최소 4개 이상의 후보 생성
     * 2. 요청에 보낸 전략을 최우선으로 적용
     * 3. 나머지는 요청에 없는 전략들을 순차적으로 적용
     */
    private List<ScheduleGenerationStrategy> getStrategiesToUse(GenerationOptionsDto options) {
        // 모든 전략 맵
        Map<GenerationOptionsDto.GenerationStrategy, ScheduleGenerationStrategy> strategyMap = Map.of(
                GenerationOptionsDto.GenerationStrategy.BALANCED, balancedStrategy,
                GenerationOptionsDto.GenerationStrategy.COVERAGE_FIRST, coverageFirstStrategy,
                GenerationOptionsDto.GenerationStrategy.SENIOR_PRIORITY, seniorPriorityStrategy,
                GenerationOptionsDto.GenerationStrategy.FAIR_DISTRIBUTION, fairDistributionStrategy
        );

        // 전체 전략 순서 (기본 순서)
        List<GenerationOptionsDto.GenerationStrategy> allStrategyOrder = List.of(
                GenerationOptionsDto.GenerationStrategy.BALANCED,
                GenerationOptionsDto.GenerationStrategy.COVERAGE_FIRST,
                GenerationOptionsDto.GenerationStrategy.SENIOR_PRIORITY,
                GenerationOptionsDto.GenerationStrategy.FAIR_DISTRIBUTION
        );

        List<ScheduleGenerationStrategy> result = new ArrayList<>();

        // 1. 요청에 보낸 전략을 최우선으로 추가
        Set<GenerationOptionsDto.GenerationStrategy> requestedStrategies = new LinkedHashSet<>();
        if (options != null && options.getStrategies() != null && !options.getStrategies().isEmpty()) {
            for (GenerationOptionsDto.GenerationStrategy strategy : options.getStrategies()) {
                if (strategyMap.containsKey(strategy)) {
                    result.add(strategyMap.get(strategy));
                    requestedStrategies.add(strategy);
                }
            }
        }

        // 2. 요청에 없는 전략들을 순차적으로 추가
        List<ScheduleGenerationStrategy> remainingStrategies = new ArrayList<>();
        for (GenerationOptionsDto.GenerationStrategy strategy : allStrategyOrder) {
            if (!requestedStrategies.contains(strategy)) {
                remainingStrategies.add(strategyMap.get(strategy));
            }
        }

        // 3. 최소 4개 보장 (요청 전략 + 나머지 전략으로 채움)
        int minCount = 4;
        int needed = minCount - result.size();

        for (int i = 0; i < needed && i < remainingStrategies.size(); i++) {
            result.add(remainingStrategies.get(i));
        }

        log.info("📋 생성할 후보 수: {}, 사용 전략: {}",
                result.size(),
                result.stream().map(ScheduleGenerationStrategy::getStrategyName).toList());

        return result;
    }

    /**
     * @deprecated 전략 패턴 기반 generateCandidatesWithStrategies 사용 권장
     */
    @Deprecated
    public List<CandidateSchedule> generateWeeklyCandidates(Long storeId,
                                                            ScheduleSettingSnapshot settings,
                                                            int candidateCount) {
        // 근무 가능자 로드
        List<WorkAvailability> availabilities = workAvailabilityRepository.findByUserStore_Store_Id(storeId);
        if (availabilities.isEmpty()) {
            throw new IllegalStateException("근무 가능 시간을 제출한 직원이 없습니다.");
        }

        List<CandidateSchedule> candidateSchedules = new ArrayList<>();

        // 직원 username 매핑
        Map<Long, String> userStoreUsernameMap = userStoreRepository
                .findUserStoreIdAndUsernameByStoreId(storeId)
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (String) row[1]
                ));

        // 직원 경력(hireDate) 매핑
        Map<Long, LocalDate> userStoreHireDateMap = userStoreRepository.findByStore_Id(storeId)
                .stream()
                .collect(Collectors.toMap(UserStore::getId, UserStore::getHireDate));

        // 후보 스케줄 생성
        for (int c = 0; c < candidateCount; c++) {
            Map<Long, Integer> assignmentCount = new HashMap<>();
            CandidateSchedule candidate = new CandidateSchedule(storeId);

            for (ScheduleSettingSnapshot.SegmentSnapshot seg : settings.getSegments()) {
                LocalTime start = seg.getStartTime();
                LocalTime end = seg.getEndTime();
                int requiredNum = seg.getRequiredStaff();

                for (DayOfWeek day : DayOfWeek.values()) {
                    Set<Long> assignedUserIds = new HashSet<>();
                    List<UserStore> availableStaffs = new ArrayList<>();

                    // 해당 요일/시간에 근무 가능한 직원 필터링
                    for (WorkAvailability wa : availabilities) {
                        if (wa.getDayOfWeek() == day &&
                                wa.getStartTime().isBefore(end) &&
                                wa.getEndTime().isAfter(start)) {
                            availableStaffs.add(wa.getUserStore());
                        }
                    }

                    // 우선순위 정렬: 1) 적게 배정된 순 (공정) 2) 경력 높은 순 (hireDate 이른 순)
                    availableStaffs.sort((u1, u2) -> {
                        int c1 = assignmentCount.getOrDefault(u1.getId(), 0);
                        int c2 = assignmentCount.getOrDefault(u2.getId(), 0);
                        if (c1 != c2) return c1 - c2;

                        LocalDate h1 = userStoreHireDateMap.getOrDefault(u1.getId(), LocalDate.now());
                        LocalDate h2 = userStoreHireDateMap.getOrDefault(u2.getId(), LocalDate.now());
                        return h1.compareTo(h2); // 경력 높은 순 (입사일 이른 순)
                    });

                    int assigned = 0;
                    for (UserStore staff : availableStaffs) {
                        if (assigned >= requiredNum) break;
                        if (assignedUserIds.contains(staff.getId())) continue;

                        assignedUserIds.add(staff.getId());
                        String username = userStoreUsernameMap.get(staff.getId());

                        candidate.addShift(new CandidateShift(
                                staff.getId(), username, day, start, end
                        ));

                        assignmentCount.merge(staff.getId(), 1, Integer::sum);
                        assigned++;
                    }

                    // 남은 자리 UNASSIGNED 처리
                    while (assigned < requiredNum) {
                        candidate.addShift(new CandidateShift(
                                null, null, day, start, end, "UNASSIGNED"
                        ));
                        assigned++;
                    }
                }
            }
            candidateSchedules.add(candidate);
        }

        return candidateSchedules;
    }

    private String saveCandidateSchedulesToRedis(Long storeId, List<CandidateSchedule> schedules) {
        String key = "candidate_schedule:store:" + storeId + ":week:" + getCurrentWeekString();

        try {
            String jsonToSave = objectMapper.writeValueAsString(schedules);
            redisTemplate.opsForValue().set(key, jsonToSave, Duration.ofDays(1));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Redis 캐시 변환 실패", e);
        }

        return key;
    }

    private ScheduleGenerationResponseDto buildResponse(ScheduleRequest request, Long storeId,
                                                        List<ScheduleSettingSnapshot.SegmentSnapshot> segments,
                                                        String redisKey, int candidateCount) {
        List<ScheduleSettingSegmentResponseDto> segmentDtos = segments.stream()
                .map(seg -> {
                    ScheduleSettingSegmentResponseDto dto = new ScheduleSettingSegmentResponseDto();
                    dto.setStartTime(seg.getStartTime());
                    dto.setEndTime(seg.getEndTime());
                    dto.setRequiredStaff(seg.getRequiredStaff());
                    return dto;
                })
                .collect(Collectors.toList());

        ScheduleGenerationResponseDto response = new ScheduleGenerationResponseDto();
        response.setStatus("success");
        response.setScheduleRequestId(request.getId());
        response.setStoreId(storeId);
        response.setTimeSegments(segmentDtos);
        response.setCandidateScheduleKey(redisKey);
        response.setGeneratedCount(candidateCount);
        return response;
    }

    private String getCurrentWeekString() {
        return LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_WEEK_DATE);
    }

    @Transactional(readOnly = true)
    public List<Long> validateAllSubmitted(Long storeId) {
        List<UserStore> userStores = userStoreRepository.findByStore_Id(storeId);
        List<Long> submittedUserStoreIds = workAvailabilityRepository.findDistinctUserStoreIdsByStoreId(storeId);

        List<Long> unsubmitted = new ArrayList<>();
        for (UserStore us : userStores) {
            if (us.getPosition() == UserStore.Position.OWNER) continue;
            if (!submittedUserStoreIds.contains(us.getId())) {
                unsubmitted.add(us.getUser().getId());
            }
        }
        return unsubmitted;
    }

    // ========================================
    // 설정 스냅샷 (StoreSetting 시간대 + StaffRequirement 인원수 결합)
    // ========================================
    @lombok.Getter
    @lombok.Builder
    public static class ScheduleSettingSnapshot {
        private LocalTime openTime;
        private LocalTime closeTime;
        private boolean useSegments;
        private boolean hasBreakTime;
        private LocalTime breakStartTime;
        private LocalTime breakEndTime;
        private List<SegmentSnapshot> segments;

        @lombok.Getter
        @lombok.Builder
        public static class SegmentSnapshot {
            private LocalTime startTime;
            private LocalTime endTime;
            private int requiredStaff;
        }

        /**
         * StoreSetting(시간대) + StaffRequirementDto(인원수) 결합
         * <p>
         * 1. 세그먼트 사용 O: 각 세그먼트별로 필요 인원수 적용
         * 2. 세그먼트 사용 X: 가게 운영 시간 전체에 필요한 동시 근무자 수 적용
         */
        public static ScheduleSettingSnapshot fromStoreSettingWithStaff(
                StoreSetting setting,
                StaffRequirementDto staffRequirement) {

            List<SegmentSnapshot> segs = new ArrayList<>();

            if (setting.isUseSegments() && setting.getSegments() != null && !setting.getSegments().isEmpty()) {
                // 세그먼트 사용 시: StoreSetting의 시간대 + StaffRequirement의 인원수 결합
                Map<Integer, Integer> staffMap = new HashMap<>();
                if (staffRequirement != null && staffRequirement.getSegmentStaffList() != null) {
                    for (StaffRequirementDto.SegmentStaffDto segStaff : staffRequirement.getSegmentStaffList()) {
                        staffMap.put(segStaff.getSegmentIndex(), segStaff.getRequiredStaff());
                    }
                }

                List<StoreSettingSegment> storeSegments = setting.getSegments();
                for (int i = 0; i < storeSegments.size(); i++) {
                    StoreSettingSegment seg = storeSegments.get(i);
                    int requiredStaff = staffMap.getOrDefault(i, 1); // 기본값 1명
                    segs.add(SegmentSnapshot.builder()
                            .startTime(seg.getStartTime())
                            .endTime(seg.getEndTime())
                            .requiredStaff(requiredStaff)
                            .build());
                }
            } else {
                // 세그먼트 미사용 시: 전체 운영시간을 하나의 세그먼트로, 동시 근무자 수 적용
                int requiredStaff = (staffRequirement != null && staffRequirement.getRequiredStaff() != null)
                        ? staffRequirement.getRequiredStaff() : 1;
                segs.add(SegmentSnapshot.builder()
                        .startTime(setting.getOpenTime())
                        .endTime(setting.getCloseTime())
                        .requiredStaff(requiredStaff)
                        .build());
            }

            return ScheduleSettingSnapshot.builder()
                    .openTime(setting.getOpenTime())
                    .closeTime(setting.getCloseTime())
                    .useSegments(setting.isUseSegments())
                    .hasBreakTime(setting.isHasBreakTime())
                    .breakStartTime(setting.getBreakStartTime())
                    .breakEndTime(setting.getBreakEndTime())
                    .segments(segs)
                    .build();
        }
    }
}
