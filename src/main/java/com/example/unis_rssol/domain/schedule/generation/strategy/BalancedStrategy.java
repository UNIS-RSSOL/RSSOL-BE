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
 * BALANCED 전략: 경력자-신입 균형 배치
 * - 2인 이상 근무 시 경력자(1년 이상) 최소 1명 배치
 * - 경력자와 신입의 조합으로 멘토링 효과 기대
 */
@Component
public class BalancedStrategy implements ScheduleGenerationStrategy {

    private static final int SENIOR_THRESHOLD_MONTHS = 12; // 1년 이상이면 경력자

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

                // 경력자/신입 분류
                List<UserStore> seniors = new ArrayList<>();
                List<UserStore> juniors = new ArrayList<>();

                for (UserStore staff : availableStaffs) {
                    LocalDate hireDate = userStoreHireDateMap.getOrDefault(staff.getId(), now);
                    long monthsWorked = java.time.temporal.ChronoUnit.MONTHS.between(hireDate, now);

                    if (monthsWorked >= SENIOR_THRESHOLD_MONTHS) {
                        seniors.add(staff);
                    } else {
                        juniors.add(staff);
                    }
                }

                // 각 그룹 내에서 적게 배정된 순으로 정렬
                Comparator<UserStore> byAssignmentCount = (u1, u2) -> {
                    int c1 = assignmentCount.getOrDefault(u1.getId(), 0);
                    int c2 = assignmentCount.getOrDefault(u2.getId(), 0);
                    return Integer.compare(c1, c2);
                };
                seniors.sort(byAssignmentCount);
                juniors.sort(byAssignmentCount);

                int assigned = 0;

                // 2인 이상 근무 시 경력자 최소 1명 배치
                if (requiredNum >= 2 && !seniors.isEmpty()) {
                    UserStore senior = seniors.remove(0);
                    assignShift(candidate, senior, day, start, end, userStoreUsernameMap, assignmentCount, assignedUserIds);
                    assigned++;
                }

                // 남은 자리에 균형있게 배치 (신입-경력 번갈아)
                Queue<UserStore> seniorQueue = new LinkedList<>(seniors);
                Queue<UserStore> juniorQueue = new LinkedList<>(juniors);
                boolean pickJunior = true; // 신입부터 번갈아 배치

                while (assigned < requiredNum) {
                    UserStore next = null;

                    if (pickJunior && !juniorQueue.isEmpty()) {
                        next = juniorQueue.poll();
                    } else if (!pickJunior && !seniorQueue.isEmpty()) {
                        next = seniorQueue.poll();
                    } else if (!juniorQueue.isEmpty()) {
                        next = juniorQueue.poll();
                    } else if (!seniorQueue.isEmpty()) {
                        next = seniorQueue.poll();
                    }

                    if (next != null && !assignedUserIds.contains(next.getId())) {
                        assignShift(candidate, next, day, start, end, userStoreUsernameMap, assignmentCount, assignedUserIds);
                        assigned++;
                    } else if (next == null) {
                        // 배정 가능한 사람 없음
                        break;
                    }

                    pickJunior = !pickJunior;
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

    private void assignShift(CandidateSchedule candidate, UserStore staff, DayOfWeek day,
                             LocalTime start, LocalTime end, Map<Long, String> usernameMap,
                             Map<Long, Integer> assignmentCount, Set<Long> assignedUserIds) {
        assignedUserIds.add(staff.getId());
        String username = usernameMap.get(staff.getId());
        candidate.addShift(new CandidateShift(staff.getId(), username, day, start, end));
        assignmentCount.merge(staff.getId(), 1, Integer::sum);
    }

    @Override
    public String getStrategyName() {
        return "BALANCED";
    }

    @Override
    public String getDescription() {
        return "경력자-신입 균형 배치 (2인 이상 근무 시 경력자 최소 1명 배치)";
    }
}

