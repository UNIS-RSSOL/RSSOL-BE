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
 * COVERAGE_FIRST 전략: 빈자리 최소화 우선
 * - 가용 인원이 적은 슬롯부터 먼저 배정
 * - 빈자리(UNASSIGNED) 최소화가 목표
 * - 희소 슬롯에 가능한 직원을 우선 배치
 */
@Component
public class CoverageFirstStrategy implements ScheduleGenerationStrategy {

    @Override
    public CandidateSchedule generate(
            Long storeId,
            ScheduleSettingSnapshot settings,
            List<WorkAvailability> availabilities,
            Map<Long, String> userStoreUsernameMap,
            Map<Long, LocalDate> userStoreHireDateMap) {

        CandidateSchedule candidate = new CandidateSchedule(storeId);
        Map<Long, Integer> assignmentCount = new HashMap<>();
        Map<Long, Set<String>> userAssignedSlots = new HashMap<>(); // 중복 배정 방지

        // 1. 모든 슬롯을 가용 인원 수 기준으로 정렬 (적은 순)
        List<SlotInfo> allSlots = new ArrayList<>();

        for (ScheduleSettingSnapshot.SegmentSnapshot seg : settings.getSegments()) {
            for (DayOfWeek day : DayOfWeek.values()) {
                List<UserStore> availableStaffs = filterAvailableStaffs(availabilities, day,
                        seg.getStartTime(), seg.getEndTime());

                allSlots.add(new SlotInfo(day, seg.getStartTime(), seg.getEndTime(),
                        seg.getRequiredStaff(), availableStaffs));
            }
        }

        // 가용 인원이 적은 슬롯부터 처리 (빈자리 발생 가능성 높은 슬롯 우선)
        allSlots.sort(Comparator.comparingInt(s -> s.availableStaffs.size()));

        // 2. 각 슬롯별로 배정 진행
        Map<String, List<CandidateShift>> slotShifts = new HashMap<>();

        for (SlotInfo slot : allSlots) {
            String slotKey = slot.day + "_" + slot.startTime + "_" + slot.endTime;
            List<CandidateShift> shifts = new ArrayList<>();
            Set<Long> assignedInSlot = new HashSet<>();

            // 해당 슬롯에서 아직 이 슬롯에 배정되지 않은 직원만 필터
            List<UserStore> candidates = slot.availableStaffs.stream()
                    .filter(staff -> {
                        Set<String> assigned = userAssignedSlots.getOrDefault(staff.getId(), new HashSet<>());
                        return !assigned.contains(slotKey);
                    })
                    .sorted((u1, u2) -> {
                        // 적게 배정된 순 우선
                        int c1 = assignmentCount.getOrDefault(u1.getId(), 0);
                        int c2 = assignmentCount.getOrDefault(u2.getId(), 0);
                        if (c1 != c2) return Integer.compare(c1, c2);
                        // 경력 높은 순 (안정성)
                        LocalDate h1 = userStoreHireDateMap.getOrDefault(u1.getId(), LocalDate.now());
                        LocalDate h2 = userStoreHireDateMap.getOrDefault(u2.getId(), LocalDate.now());
                        return h1.compareTo(h2);
                    })
                    .collect(Collectors.toList());

            int assigned = 0;
            for (UserStore staff : candidates) {
                if (assigned >= slot.requiredStaff) break;
                if (assignedInSlot.contains(staff.getId())) continue;

                assignedInSlot.add(staff.getId());
                String username = userStoreUsernameMap.get(staff.getId());
                shifts.add(new CandidateShift(staff.getId(), username, slot.day, slot.startTime, slot.endTime));

                assignmentCount.merge(staff.getId(), 1, Integer::sum);
                userAssignedSlots.computeIfAbsent(staff.getId(), k -> new HashSet<>()).add(slotKey);
                assigned++;
            }

            // 남은 자리 UNASSIGNED
            while (assigned < slot.requiredStaff) {
                shifts.add(new CandidateShift(null, null, slot.day, slot.startTime, slot.endTime, "UNASSIGNED"));
                assigned++;
            }

            slotShifts.put(slotKey, shifts);
        }

        // 3. 결과를 CandidateSchedule에 추가 (요일/시간 순서대로)
        for (ScheduleSettingSnapshot.SegmentSnapshot seg : settings.getSegments()) {
            for (DayOfWeek day : DayOfWeek.values()) {
                String slotKey = day + "_" + seg.getStartTime() + "_" + seg.getEndTime();
                List<CandidateShift> shifts = slotShifts.get(slotKey);
                if (shifts != null) {
                    shifts.forEach(candidate::addShift);
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
        return "COVERAGE_FIRST";
    }

    @Override
    public String getDescription() {
        return "빈자리 최소화 우선 (가용 인원 적은 슬롯부터 배정)";
    }

    /**
     * 슬롯 정보 내부 클래스
     */
    private static class SlotInfo {
        DayOfWeek day;
        LocalTime startTime;
        LocalTime endTime;
        int requiredStaff;
        List<UserStore> availableStaffs;

        SlotInfo(DayOfWeek day, LocalTime startTime, LocalTime endTime,
                 int requiredStaff, List<UserStore> availableStaffs) {
            this.day = day;
            this.startTime = startTime;
            this.endTime = endTime;
            this.requiredStaff = requiredStaff;
            this.availableStaffs = availableStaffs;
        }
    }
}

