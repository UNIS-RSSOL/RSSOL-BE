package com.example.unis_rssol.schedule.generation;

import com.example.unis_rssol.global.auth.AuthorizationService;
import com.example.unis_rssol.global.exception.ForbiddenException;
import com.example.unis_rssol.global.exception.NotFoundException;
import com.example.unis_rssol.schedule.DayOfWeek;
import com.example.unis_rssol.schedule.generation.dto.ScheduleGenerationRequestDto;
import com.example.unis_rssol.schedule.generation.entity.*;
import com.example.unis_rssol.schedule.generation.dto.*;
import com.example.unis_rssol.schedule.generation.dto.candidate.CandidateSchedule;
import com.example.unis_rssol.schedule.generation.dto.candidate.CandidateShift;
import com.example.unis_rssol.schedule.generation.dto.setting.ScheduleSettingSegmentRequestDto;
import com.example.unis_rssol.schedule.generation.dto.setting.ScheduleSettingSegmentResponseDto;
import com.example.unis_rssol.schedule.notification.NotificationService;
import com.example.unis_rssol.schedule.repository.ScheduleRepository;
import com.example.unis_rssol.schedule.repository.ScheduleSettingsRepository;
import com.example.unis_rssol.schedule.repository.WorkShiftRepository;
import com.example.unis_rssol.schedule.workavailability.WorkAvailability;
import com.example.unis_rssol.schedule.workavailability.WorkAvailabilityRepository;
import com.example.unis_rssol.domain.store.entity.Store;
import com.example.unis_rssol.domain.store.entity.UserStore;
import com.example.unis_rssol.domain.store.repository.StoreRepository;
import com.example.unis_rssol.domain.store.repository.UserStoreRepository;
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
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.example.unis_rssol.domain.store.entity.UserStore.Position.OWNER;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleGenerationService {
    private final ScheduleSettingsRepository scheduleSettingsRepository;
    private final StoreRepository storeRepository;
    private final AuthorizationService authService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final WorkAvailabilityRepository workAvailabilityRepository;
    private final UserStoreRepository userStoreRepository;
    private final ScheduleRepository scheduleRepository;
    private final WorkShiftRepository workShiftRepository;
    private final NotificationService notificationService;

    public void testRedis(List<CandidateSchedule> candidateSchedules) throws JsonProcessingException {
        String key = "candidate_schedule_test";
        ObjectMapper mapper = new ObjectMapper();

        // 저장: JSON 문자열로 변환 후 저장
        String jsonToSave = mapper.writeValueAsString(candidateSchedules);
        redisTemplate.opsForValue().set(key, jsonToSave, Duration.ofDays(1));

        // 읽기: JSON 문자열을 가져와서 객체로 변환
        String jsonFromRedis = (String) redisTemplate.opsForValue().get(key);
        List<CandidateSchedule> readList = mapper.readValue(
                jsonFromRedis,
                new TypeReference<>() {
                }
        );

        // 이제 readList 사용 가능
        readList.forEach(System.out::println);
    }
    //세팅&알림
    @Transactional
    public ScheduleRequestResponseDto requestSchedule(Long userId, ScheduleRequestDto request) {
        Long storeId = authService.getActiveStoreIdOrThrow(userId);
        UserStore owner = authService.getUserStoreOrThrow(userId, storeId);
        Store store = storeRepository.findById(storeId).orElseThrow(() -> new NotFoundException("존재하지 않는 매장입니다."));

        if (owner.getPosition() != OWNER) {
            throw new ForbiddenException("해당 매장의 근무표를 생성할 권한이 없습니다.");
        }

        // 1. ScheduleSettings 저장 + Segment(구간) 생성
        ScheduleSettings settings = createOrUpdateSetting(store, request);
        createSegmentsFromRequest(settings, request.getTimeSegments());

        // 2. 상태 저장(Requested!) :요청중입니다.
        settings.setStatus(ScheduleSettings.ScheduleStatus.REQUESTED);

        // 3. 알림 생성
        notificationService.sendScheduleInputRequest(storeId, request.getStartDate(),request.getEndDate());

        return new ScheduleRequestResponseDto(settings.getId(), settings.getStatus().name());
    }

    //계산&생성
    @Transactional
    public ScheduleGenerationResponseDto generateSchedule(Long userId, Long settingId, ScheduleGenerationRequestDto request){
        Long storeId = authService.getActiveStoreIdOrThrow(userId);
        ScheduleSettings setting = scheduleSettingsRepository.findById(settingId).orElseThrow(() -> new NotFoundException("해당 매장의 스케줄 세팅이 없습니다."));

        if(setting.getStatus() != ScheduleSettings.ScheduleStatus.REQUESTED){ throw new IllegalStateException("아직 요청 상태가 아닙니다.");}

        // 1️. 근무 가능 시간 모두 제출됐는지 확인
        List<Long> unsubmitted = validateAllSubmitted(storeId);
        if (!unsubmitted.isEmpty()) {throw new IllegalStateException("아직 근무 시간표를 제출하지 않은 직원이 있습니다.");}


        //2. 생성
        List<CandidateSchedule> candidates = generateWeeklyCandidates(storeId, setting, request.getGenerationOptions().getCandidateCount());

        setting.setStatus(ScheduleSettings.ScheduleStatus.GENERATED);
        String redisKey = saveCandidateSchedulesToRedis(storeId, candidates);

        // Response 생성
        return buildResponse(setting, storeId, setting.getSegments(), redisKey, candidates.size());
    }

    // Redis에서 읽어올 때도 항상 JSON → 객체 변환
    public List<CandidateSchedule> getCandidateSchedules(String redisKey) {
        String jsonFromRedis = (String) redisTemplate.opsForValue().get(redisKey);
        if (jsonFromRedis == null) throw new NotFoundException("생성된 근무표가 없습니다.");

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        try {
            return mapper.readValue(jsonFromRedis, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Redis 캐시 읽기 실패", e);
        }
    }

    @Transactional
    public Schedule finalizeCandidateSchedule(Long storeId, String candidateKey,
                                              LocalDate startDate, LocalDate endDate) {
        // 1️⃣ Schedule 엔티티 생성
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new NotFoundException("매장을 찾을 수 없습니다."));
        Schedule schedule = new Schedule();
        schedule.setStore(store);
        schedule.setStartDate(startDate);
        schedule.setEndDate(endDate);

        // 2️⃣ Redis에서 CandidateSchedule 가져오기
        List<CandidateSchedule> candidates = getCandidateSchedules(candidateKey);

        if (candidates.isEmpty()) {
            throw new IllegalStateException("CandidateSchedule이 없습니다.");
        }

        // 3️⃣ 여기서는 첫 번째 후보를 확정한다고 가정
        CandidateSchedule selected = candidates.get(0);

        // 4️⃣ CandidateShift → WorkShift 변환
        for (CandidateShift shift : selected.getShifts()) {
            if (shift.getUserStoreId() == null) continue; // UNASSIGNED

            WorkShift ws = new WorkShift();
            ws.setUserStore(userStoreRepository.findById(shift.getUserStoreId())
                    .orElseThrow(() -> new NotFoundException("직원 정보를 찾을 수 없습니다.")));
            ws.setStore(store);

            //연관관계 조인 설정
            ws.setSchedule(schedule);
            schedule.getWorkShifts().add(ws);

            // startDate 기준 + 요일 offset 계산
            LocalDate shiftDate = startDate.plusDays(shift.getDay().getValue() - 1); // MON=1
            ws.setStartDatetime(shiftDate.atTime(shift.getStartTime()));
            ws.setEndDatetime(shiftDate.atTime(shift.getEndTime()));


        }

        // 5️⃣ Schedule 최종 확정, cascade = ALL 이므로 WorkShift 자동 persist
        Schedule saved = scheduleRepository.save(schedule);
        redisTemplate.delete(candidateKey);
        return saved;
    }



    // ==========================================================================
    @Transactional
    protected ScheduleSettings createOrUpdateSetting(Store store, ScheduleRequestDto request) {
        Optional<ScheduleSettings> existingOpt = scheduleSettingsRepository.findByStoreId(store.getId());

        ScheduleSettings scheduleSettings;
        if (existingOpt.isPresent()) {
            scheduleSettings = existingOpt.get();
            // 기존 엔티티 업데이트
            scheduleSettings.setOpenTime(request.getOpenTime());
            scheduleSettings.setCloseTime(request.getCloseTime());
            scheduleSettings.getSegments().clear(); // segments 새로 설정
        } else {
            scheduleSettings = new ScheduleSettings(store, request.getOpenTime(), request.getCloseTime());
        }

        return scheduleSettingsRepository.save(scheduleSettings);         // 저장까지 수행
    }

    @Transactional
    protected List<ScheduleSettingSegment> createSegmentsFromRequest(ScheduleSettings scheduleSettings, List<ScheduleSettingSegmentRequestDto> requestSegments) {
        List<ScheduleSettingSegment> segments = new ArrayList<>();

        for (ScheduleSettingSegmentRequestDto ts : requestSegments) {
            ScheduleSettingSegment segment = new ScheduleSettingSegment();
            segment.setScheduleSettings(scheduleSettings);
            segment.setStartTime(ts.getStartTime());
            segment.setEndTime(ts.getEndTime());
            segment.setRequiredStaff(ts.getRequiredStaff());
            segments.add(segment);
        }

        scheduleSettings.getSegments().addAll(segments); // 기존 segments clear 후 새로 추가
        return segments;
    }


    public List<CandidateSchedule> generateWeeklyCandidates(Long storeId, ScheduleSettings settings, int candidateCount) {
//        ObjectMapper mapper = new ObjectMapper();
//        mapper.registerModule(new JavaTimeModule());
//        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 1️.  근무 가능자 로드
        List<WorkAvailability> availabilities = workAvailabilityRepository.findByUserStore_Store_Id(storeId);
        if (availabilities.isEmpty()) {
            throw new IllegalStateException("근무 가능 시간을 제출한 직원이 없습니다.");
        }

        // 2️, 후보 스케줄 리스트
        List<CandidateSchedule> candidateSchedules = new ArrayList<>();
        // 3️. 직원 배정 횟수 기록 (공정 배분용)
        Map<Long, String> userStoreUsernameMap =
                userStoreRepository.findUserStoreIdAndUsernameByStoreId(storeId)
                        .stream()
                        .collect(Collectors.toMap(
                                row -> (Long) row[0],   // user_store.id
                                row -> (String) row[1]  // username
                        ));

        // 4️. 후보 스케줄 여러 개 생성
        for (int c = 0; c < candidateCount; c++) {
            Map<UserStore, Integer> assignmentCount = new HashMap<>();
            CandidateSchedule candidate = new CandidateSchedule(storeId);

            // 1일 기준 Segment 반복
            for (ScheduleSettingSegment seg : settings.getSegments()) {
                LocalTime start = seg.getStartTime();
                LocalTime end = seg.getEndTime();
                int requiredNum = seg.getRequiredStaff();

                for (DayOfWeek day : DayOfWeek.values()) { // MON~SUN
                    Set<String> assignedKeySet = new HashSet<>();
                    List<UserStore> availableStaffs = new ArrayList<>();
                    for (WorkAvailability wa : availabilities) {
                        if (wa.getDayOfWeek() == day &&
                                wa.getStartTime().isBefore(end) && wa.getEndTime().isAfter(start)) {
                            availableStaffs.add(wa.getUserStore());
                        }
                    }

                    // 공정 + 경력순 정렬
                    availableStaffs.sort((u1, u2) -> {
                        int c1 = assignmentCount.getOrDefault(u1, 0);
                        int c2 = assignmentCount.getOrDefault(u2, 0);
                        if (c1 != c2) return c1 - c2; // 적게 배정된 순
                        return u1.getHireDate().compareTo(u2.getHireDate()); // 경력순
                    });

                    int assigned = 0;
                    for (UserStore staff : availableStaffs) {
                        if (assigned >= requiredNum) break;

                        String key = day + "-" + staff.getId();
                        if (assignedKeySet.contains(key)) continue;

                        assignedKeySet.add(key);
                        String username = userStoreUsernameMap.get(staff.getId());

                        candidate.addShift(
                                new CandidateShift(staff.getId(), username, day, start, end)
                        );

                        assignmentCount.put(staff,
                                assignmentCount.getOrDefault(staff, 0) + 1);
                        assigned++;
                    }

                    // 남은 자리 UNASSIGNED 처리
                    while (assigned < requiredNum) {
                        CandidateShift shift = new CandidateShift(null, null,day, start, end, "UNASSIGNED");
                        candidate.addShift(shift);
                        assigned++;
                    }
                }
            }
            candidateSchedules.add(candidate);
        }
        //
        saveCandidateSchedulesToRedis(storeId, candidateSchedules);

        return candidateSchedules;
    }


    private String saveCandidateSchedulesToRedis(Long storeId, List<CandidateSchedule> schedules) {
        String key = "candidate_schedule:store:" + storeId + ":week:" + getCurrentWeekString();
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        try {
            String jsonToSave = mapper.writeValueAsString(schedules);
            redisTemplate.opsForValue().set(key, jsonToSave, Duration.ofDays(1));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Redis 캐시 변환 실패", e);
        }

        return key;
    }

    private ScheduleGenerationResponseDto buildResponse(ScheduleSettings settings, Long storeId,
                                                        List<ScheduleSettingSegment> segments,
                                                        String redisKey, int candidateCount) {

        List<ScheduleSettingSegmentResponseDto> segmentDtos = new ArrayList<>();
        for (ScheduleSettingSegment seg : segments) {
            ScheduleSettingSegmentResponseDto dto = new ScheduleSettingSegmentResponseDto();
            dto.setId(seg.getId());
            dto.setStartTime(seg.getStartTime());
            dto.setEndTime(seg.getEndTime());
            dto.setRequiredStaff(seg.getRequiredStaff());
            segmentDtos.add(dto);
        }

        ScheduleGenerationResponseDto response = new ScheduleGenerationResponseDto();
        response.setStatus("success");
        response.setScheduleSettingsId(settings.getId());
        response.setStoreId(storeId);
        response.setTimeSegments(segmentDtos);
        response.setCandidateScheduleKey(redisKey);
        response.setGeneratedCount(candidateCount);
        return response;
    }

    private String getCurrentWeekString() {
        return java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_WEEK_DATE);
    }

    @Transactional(readOnly = true)
    public List<Long> validateAllSubmitted(Long storeId) {
        // 매장 소속 직원
        List<UserStore> userStores = userStoreRepository.findByStore_Id(storeId);

        // 근무 가능 시간 제출한 직원 ID 목록
        List<Long> submittedUserStoreIds = workAvailabilityRepository.findDistinctUserStoreIdsByStoreId(storeId);

        List<Long> unsubmitted = new ArrayList<>();
        for (UserStore us : userStores) {
            if (us.getPosition() == UserStore.Position.OWNER) continue; // 사장 제외
            if (!submittedUserStoreIds.contains(us.getId())) {unsubmitted.add(us.getUser().getId());}
        }

        return unsubmitted;
    }



}
