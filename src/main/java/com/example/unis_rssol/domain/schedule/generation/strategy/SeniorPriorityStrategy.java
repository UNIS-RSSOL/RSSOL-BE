package com.example.unis_rssol.domain.schedule.generation.strategy;

import com.example.unis_rssol.domain.schedule.DayOfWeek;
import com.example.unis_rssol.domain.schedule.generation.ScheduleGenerationService.ScheduleSettingSnapshot;
import com.example.unis_rssol.domain.schedule.generation.dto.candidate.CandidateSchedule;
import com.example.unis_rssol.domain.schedule.generation.dto.candidate.CandidateShift;
import com.example.unis_rssol.domain.schedule.workavailability.WorkAvailability;
import com.example.unis_rssol.domain.store.UserStore;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * SENIOR_PRIORITY 전략: 경력자 우선 배치
 * - 경력이 많은 직원(입사일이 이른)을 우선 배정
 * - 안정적인 운영을 위해 숙련된 직원 중심 배치
 * - 신입은 경력자 배치 후 남은 자리에 배정
 */
@Component
public class SeniorPriorityStrategy implements ScheduleGenerationStrategy {

    @Override
    public CandidateSchedule generate(
            Long storeId,
            ScheduleSettingSnapshot settings,
            List<WorkAvailability> availabilities,
            Map<Long, String> userStoreUsernameMap,
            Map<Long, LocalDate> userStoreHireDateMap) {

        CandidateSchedule candidate = new CandidateSchedule(storeId);
        Map<Long, Integer> assignmentCount = new HashMap<>();
        LocalDate now = LocalDate.now();

        for (ScheduleSettingSnapshot.SegmentSnapshot seg : settings.getSegments()) {
            LocalTime start = seg.getStartTime();
            LocalTime end = seg.getEndTime();
            int requiredNum = seg.getRequiredStaff();

            for (DayOfWeek day : DayOfWeek.values()) {
                Set<Long> assignedUserIds = new HashSet<>();

                // 해당 요일/시간에 근무 가능한 직원 필터링
                List<UserStore> availableStaffs = filterAvailableStaffs(availabilities, day, start, end);

                // 경력 순 정렬 (입사일 이른 순 = 경력 많은 순)
                // 동일 경력이면 적게 배정된 순
                availableStaffs.sort((u1, u2) -> {
                    LocalDate h1 = userStoreHireDateMap.getOrDefault(u1.getId(), now);
                    LocalDate h2 = userStoreHireDateMap.getOrDefault(u2.getId(), now);
                    int hireDateCompare = h1.compareTo(h2);
                    if (hireDateCompare != 0) return hireDateCompare;

                    int c1 = assignmentCount.getOrDefault(u1.getId(), 0);
                    int c2 = assignmentCount.getOrDefault(u2.getId(), 0);
                    return Integer.compare(c1, c2);
                });

                int assigned = 0;
                for (UserStore staff : availableStaffs) {
                    if (assigned >= requiredNum) break;
                    if (assignedUserIds.contains(staff.getId())) continue;

                    assignedUserIds.add(staff.getId());
                    String username = userStoreUsernameMap.get(staff.getId());
                    candidate.addShift(new CandidateShift(staff.getId(), username, day, start, end));
                    assignmentCount.merge(staff.getId(), 1, Integer::sum);
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
        return "SENIOR_PRIORITY";
    }

    @Override
    public String getDescription() {
        return "경력자 우선 배치 (숙련된 직원 중심 안정적 운영)";
    }
}

