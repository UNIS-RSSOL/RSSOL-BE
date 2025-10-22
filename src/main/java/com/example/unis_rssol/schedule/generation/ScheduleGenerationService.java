package com.example.unis_rssol.schedule.generation;

import com.example.unis_rssol.global.AuthorizationService;
import com.example.unis_rssol.global.exception.ForbiddenException;
import com.example.unis_rssol.global.exception.NotFoundException;
import com.example.unis_rssol.schedule.DayOfWeek;
import com.example.unis_rssol.schedule.entity.ScheduleSettingSegment;
import com.example.unis_rssol.schedule.entity.ScheduleSettings;
import com.example.unis_rssol.schedule.generation.dto.*;
import com.example.unis_rssol.schedule.repository.ScheduleSettingsRepository;
import com.example.unis_rssol.schedule.workavailability.WorkAvailability;
import com.example.unis_rssol.schedule.workavailability.WorkAvailabilityRepository;
import com.example.unis_rssol.store.entity.Store;
import com.example.unis_rssol.store.entity.UserStore;
import com.example.unis_rssol.store.repository.StoreRepository;
import com.example.unis_rssol.store.repository.UserStoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalTime;
import java.util.*;

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

    @Transactional
    public ScheduleGenerationResponseDto createSchedules(Long userId, ScheduleGenerationRequestDto request) {
        Long storeId = authService.getActiveStoreIdOrThrow(userId);
        UserStore requester = authService.getUserStoreOrThrow(userId, storeId);
        Store store = storeRepository.findById(storeId).orElseThrow(() -> new NotFoundException("존재하지 않는 매장입니다."));

        if (requester.getPosition() != UserStore.Position.OWNER) {
            throw new ForbiddenException("해당 매장의 근무표를 생성할 권한이 없습니다.");
        }

        // 1. schedule setting 생성 또는 가져오기
        ScheduleSettings scheduleSettings = getOrCreateScheduleSettingExist(store, request);
        // 2. 요청에서 timeSegment 가져와서 ScheduleSettingSement 생성 및 저장 (근무표 생성 뼈대 제작 완료! - 매장 근무인원 설정 등임)
        List<ScheduleSettingSegment> segments = createSegmentsFromRequest(scheduleSettings, request.getTimeSegments());


        // 3️.  후보 스케줄 생성 및 Redis 저장
        List<CandidateSchedule> candidates = generateWeeklyCandidates(storeId, scheduleSettings, request.getGenerationOptions().getCandidateCount());
        String redisKey = saveCandidateSchedulesToRedis(storeId, candidates);


        // 6️⃣ Response 생성
        return buildResponse(scheduleSettings, storeId, segments, redisKey, candidates.size());


    }

// ==========================================================================

    private ScheduleSettings getOrCreateScheduleSettingExist(Store store, ScheduleGenerationRequestDto request) {
        log.info("request.getOpenTime() = " + request.getOpenTime());
        log.info("request.getCloseTime() = " + request.getCloseTime());
        Optional<ScheduleSettings> existing = scheduleSettingsRepository.findByStoreId(store.getId()); //못찾으면 Optional.empty를 반환하므로 안전하게 처리
        if (existing.isPresent()) {
            ScheduleSettings prev = existing.get();
            if (prev.getOpenTime().equals(request.getOpenTime()) && prev.getCloseTime().equals(request.getCloseTime())) {
                return prev; // 기존 설정 유지 → 세그먼트만 필요 시 업데이트
            }
        }
        return new ScheduleSettings(store, request.getOpenTime(), request.getCloseTime());                 // 최초 생성
    }

    private List<ScheduleSettingSegment> createSegmentsFromRequest(ScheduleSettings scheduleSettings, List<ScheduleSettingSegmentRequestDto> requestSegments) {
        List<ScheduleSettingSegment> segments = new ArrayList<>();

        for (ScheduleSettingSegmentRequestDto ts : requestSegments) {
            ScheduleSettingSegment segment = new ScheduleSettingSegment();
            segment.setScheduleSettings(scheduleSettings);
            segment.setStartTime(ts.getStartTime());
            segment.setEndTime(ts.getEndTime());
            segment.setRequiredStaff(ts.getRequiredStaff());
            segments.add(segment);
        }

        scheduleSettings.setSegments(segments);
        scheduleSettingsRepository.save(scheduleSettings);
        return segments;
    }


    public List<CandidateSchedule> generateWeeklyCandidates(Long storeId,ScheduleSettings settings, int candidateCount) {

        // 1️.  근무 가능자 로드
        List<WorkAvailability> availabilities = workAvailabilityRepository.findByUserStore_Store_Id(storeId);
        if (availabilities.isEmpty()) {
            throw new IllegalStateException("근무 가능 시간을 제출한 직원이 없습니다.");
        }

        // 2️, 후보 스케줄 리스트
        List<CandidateSchedule> candidateSchedules = new ArrayList<>();
        // 3️. 직원 배정 횟수 기록 (공정 배분용)
        Map<UserStore, Integer> assignmentCount = new HashMap<>();

        // 4️. 후보 스케줄 여러 개 생성
        for (int c = 0; c < candidateCount; c++) {
            CandidateSchedule candidate = new CandidateSchedule(storeId);

            // 1일 기준 Segment 반복
            for (ScheduleSettingSegment seg : settings.getSegments()) {
                LocalTime start = seg.getStartTime();
                LocalTime end = seg.getEndTime();
                int requiredNum = seg.getRequiredStaff();

                for (DayOfWeek day : DayOfWeek.values()) { // MON~SUN
                    List<UserStore> availableStaffs = new ArrayList<>();
                    for (WorkAvailability wa : availabilities) {
                        if (wa.getDayOfWeek() == day &&
                                wa.getStartTime().isBefore(end) && wa.getEndTime().isAfter(start)) {
                            availableStaffs.add(wa.getUserStore());
                        }
                    }

                    // 공정 + 경력순 정렬
                    Collections.sort(availableStaffs, new Comparator<UserStore>() {
                        @Override
                        public int compare(UserStore u1, UserStore u2) {
                            int c1 = assignmentCount.getOrDefault(u1, 0);
                            int c2 = assignmentCount.getOrDefault(u2, 0);
                            if (c1 != c2) return c1 - c2; // 적게 배정된 순
                            return u1.getHireDate().compareTo(u2.getHireDate()); // 경력순
                        }
                    });

                    int assigned = 0;
                    for (int i = 0; i < availableStaffs.size() && assigned < requiredNum; i++) {
                        UserStore staff = availableStaffs.get(i);

                        // 이미 배정 여부 체크
                        boolean alreadyAssigned = false;
                        for (CandidateShift sh : candidate.getShifts()) {
                            if (sh.getDay() == day && staff.getId().equals(sh.getUserStoreId())) {
                                alreadyAssigned = true;
                                break;
                            }
                        }

                        if (!alreadyAssigned) {
                            CandidateShift shift = new CandidateShift(staff.getId(), day, start, end);
                            candidate.addShift(shift);
                            assignmentCount.put(staff, assignmentCount.getOrDefault(staff, 0) + 1);
                            assigned++;
                        }
                    }

                    // 남은 자리 UNASSIGNED 처리
                    while (assigned < requiredNum) {
                        CandidateShift shift = new CandidateShift(null, day, start, end, "UNASSIGNED");
                        candidate.addShift(shift);
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
        redisTemplate.opsForValue().set(key, schedules, Duration.ofDays(1));
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


}
