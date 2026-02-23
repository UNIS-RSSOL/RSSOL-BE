package com.example.unis_rssol.domain.schedule.generation.strategy;

import com.example.unis_rssol.domain.schedule.DayOfWeek;
import com.example.unis_rssol.domain.schedule.generation.ScheduleGenerationService.ScheduleSettingSnapshot;
import com.example.unis_rssol.domain.schedule.generation.dto.candidate.CandidateSchedule;
import com.example.unis_rssol.domain.schedule.generation.dto.candidate.CandidateShift;
import com.example.unis_rssol.domain.schedule.workavailability.WorkAvailability;
import com.example.unis_rssol.domain.store.UserStore;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * FAIR_DISTRIBUTION 전략: 근무시간 공정 배분
 * - 모든 직원의 총 근무시간 편차 최소화
 * - 가장 적게 배정된 직원 우선 배정
 * - 공정한 근무 배분으로 직원 만족도 향상
 */
@Component
public class FairDistributionStrategy implements ScheduleGenerationStrategy {

    @Override
    public CandidateSchedule generate(
            Long storeId,
            ScheduleSettingSnapshot settings,
            List<WorkAvailability> availabilities,
            Map<Long, String> userStoreUsernameMap,
            Map<Long, LocalDate> userStoreHireDateMap) {

        CandidateSchedule candidate = new CandidateSchedule(storeId);

        // 총 근무 시간(분) 추적
        Map<Long, Long> totalWorkMinutes = new HashMap<>();

        // 모든 가용 직원 초기화
        Set<Long> allStaffIds = availabilities.stream()
                .map(wa -> wa.getUserStore().getId())
                .collect(Collectors.toSet());
        allStaffIds.forEach(id -> totalWorkMinutes.put(id, 0L));

        for (ScheduleSettingSnapshot.SegmentSnapshot seg : settings.getSegments()) {
            LocalTime start = seg.getStartTime();
            LocalTime end = seg.getEndTime();
            int requiredNum = seg.getRequiredStaff();
            long slotMinutes = Duration.between(start, end).toMinutes();

            for (DayOfWeek day : DayOfWeek.values()) {
                Set<Long> assignedUserIds = new HashSet<>();

                // 해당 요일/시간에 근무 가능한 직원 필터링
                List<UserStore> availableStaffs = filterAvailableStaffs(availabilities, day, start, end);

                // 총 근무시간이 적은 순으로 정렬 (공정 배분)
                availableStaffs.sort((u1, u2) -> {
                    long m1 = totalWorkMinutes.getOrDefault(u1.getId(), 0L);
                    long m2 = totalWorkMinutes.getOrDefault(u2.getId(), 0L);
                    if (m1 != m2) return Long.compare(m1, m2);

                    // 동일 근무시간이면 경력 낮은 순 (신입에게 기회 부여)
                    LocalDate h1 = userStoreHireDateMap.getOrDefault(u1.getId(), LocalDate.now());
                    LocalDate h2 = userStoreHireDateMap.getOrDefault(u2.getId(), LocalDate.now());
                    return h2.compareTo(h1); // 입사일 늦은 순 = 신입
                });

                int assigned = 0;
                for (UserStore staff : availableStaffs) {
                    if (assigned >= requiredNum) break;
                    if (assignedUserIds.contains(staff.getId())) continue;

                    assignedUserIds.add(staff.getId());
                    String username = userStoreUsernameMap.get(staff.getId());
                    candidate.addShift(new CandidateShift(staff.getId(), username, day, start, end));

                    // 근무시간 누적
                    totalWorkMinutes.merge(staff.getId(), slotMinutes, Long::sum);
                    assigned++;
                }

                // 남은 자리 UNASSIGNED 처리
                while (assigned < requiredNum) {
                    candidate.addShift(new CandidateShift(null, null, day, start, end, "UNASSIGNED"));
                    assigned++;
                }
            }
        }

        return candidate;
    }

    private List<UserStore> filterAvailableStaffs(List<WorkAvailability> availabilities,
                                                  DayOfWeek day, LocalTime start, LocalTime end) {
        return availabilities.stream()
                .filter(wa -> wa.getDayOfWeek() == day &&
                        wa.getStartTime().isBefore(end) &&
                        wa.getEndTime().isAfter(start))
                .map(WorkAvailability::getUserStore)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public String getStrategyName() {
        return "FAIR_DISTRIBUTION";
    }

    @Override
    public String getDescription() {
        return "근무시간 공정 배분 (총 근무시간 편차 최소화)";
    }
}

