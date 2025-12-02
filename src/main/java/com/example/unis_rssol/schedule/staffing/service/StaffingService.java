package com.example.unis_rssol.schedule.staffing.service;

import com.example.unis_rssol.schedule.entity.Schedule;
import com.example.unis_rssol.schedule.entity.WorkShift;
import com.example.unis_rssol.schedule.repository.ScheduleRepository;
import com.example.unis_rssol.schedule.repository.WorkShiftRepository;
import com.example.unis_rssol.schedule.shift.entity.Notification;
import com.example.unis_rssol.schedule.shift.repository.NotificationRepository;
import com.example.unis_rssol.schedule.staffing.dto.*;
import com.example.unis_rssol.staffing.dto.*;
import com.example.unis_rssol.schedule.staffing.entity.StaffingRequest;
import com.example.unis_rssol.schedule.staffing.entity.StaffingResponse;
import com.example.unis_rssol.schedule.staffing.repository.StaffingRequestRepository;
import com.example.unis_rssol.schedule.staffing.repository.StaffingResponseRepository;
import com.example.unis_rssol.domain.store.entity.UserStore;
import com.example.unis_rssol.domain.store.entity.UserStore.Position;
import com.example.unis_rssol.domain.store.repository.UserStoreRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StaffingService {

    private final StaffingRequestRepository requestRepo;
    private final StaffingResponseRepository responseRepo;
    private final WorkShiftRepository workShiftRepo;
    private final ScheduleRepository scheduleRepo;
    private final UserStoreRepository userStoreRepo;
    private final NotificationRepository notificationRepo;

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // 1. 사장님 추가 인력 요청 (해당 시간대에 전혀 겹치지 않는 알바에게만 알림)
    @Transactional
    public StaffingRequestDetailDto create(Long ownerUserId, StaffingCreateDto dto) {
        UserStore ownerStore = userStoreRepo.findByUser_Id(ownerUserId).stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("사장님의 소속 매장을 찾을 수 없습니다."));

        WorkShift baseShift = workShiftRepo.findById(dto.getShiftId())
                .orElseThrow(() -> new RuntimeException("기준 근무(shiftId=" + dto.getShiftId() + ")를 찾을 수 없습니다."));

        // 매장 내 전체 STAFF
        List<UserStore> allStaff = userStoreRepo.findByStore_IdAndPosition(
                ownerStore.getStore().getId(), Position.STAFF
        );

        // 해당 시간대에 1초라도 겹치는 알바 제외
        List<Long> receiverUserIds = allStaff.stream()
                .filter(staff -> !workShiftRepo.existsByUserStore_IdAndStartDatetimeLessThanAndEndDatetimeGreaterThan(
                        staff.getId(),
                        baseShift.getEndDatetime(),
                        baseShift.getStartDatetime()
                ))
                .map(s -> s.getUser().getId())
                .collect(Collectors.toList());

        StaffingRequest request = StaffingRequest.builder()
                .store(ownerStore.getStore())
                .owner(ownerStore)
                .baseShiftId(baseShift.getId())
                .startDatetime(baseShift.getStartDatetime())
                .endDatetime(baseShift.getEndDatetime())
                .headcountRequested(dto.getHeadcount())
                .headcountFilled(0)
                .status(StaffingRequest.Status.OPEN)
                .note(dto.getNote())
                .receiverUserIds(receiverUserIds.stream().map(String::valueOf).collect(Collectors.joining(",")))
                .build();

        requestRepo.save(request);

        // 알림 전송 (필터된 대상 - 인력 요청을 할 알바생들에게만)
        String inviteMsg = buildStaffInviteMessage(request);
        for (Long receiverId : receiverUserIds) {
            notificationRepo.save(Notification.builder()
                    .userId(receiverId)
                    .category(Notification.Category.STAFFING)
                    .targetType(Notification.TargetType.STAFFING_REQUEST)
                    .targetId(request.getId())
                    .staffingRequestId(request.getId())
                    .type(Notification.Type.STAFFING_REQUEST_INVITE)
                    .message(inviteMsg)
                    .build());
        }
        return toRequestDetailDto(request);
    }

    // 2. 알바생 - 추가 인력 요청에 대해 1차 수락/거절 응답
    @Transactional
    public StaffingResponseDetailDto respond(Long userId, Long requestId, StaffingRespondDto dto) {
        StaffingRequest request = requestRepo.findById(requestId)
                .orElseThrow(() -> new RuntimeException("인력 요청을 찾을 수 없습니다."));

        UserStore candidate = userStoreRepo.findByUser_Id(userId).stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("해당 사용자의 소속 매장을 찾을 수 없습니다."));

        if (responseRepo.existsByStaffingRequest_IdAndCandidate_Id(requestId, candidate.getId())) {
            throw new RuntimeException("이미 응답한 요청입니다.");
        }

        StaffingResponse.WorkerAction action = parseWorkerAction(dto.getAction());

        StaffingResponse response = StaffingResponse.builder()
                .staffingRequest(request)
                .candidate(candidate)
                .workerAction(action)
                .managerApproval(StaffingResponse.ManagerApproval.PENDING)
                .build();
        responseRepo.save(response);

        String notifyMgrMsg = buildManagerNotifyMessage(request, response);
        notificationRepo.save(Notification.builder()
                .userId(request.getOwner().getUser().getId())
                .category(Notification.Category.STAFFING)
                .targetType(Notification.TargetType.STAFFING_RESPONSE)
                .targetId(response.getId())
                .staffingRequestId(request.getId())
                .type(Notification.Type.STAFFING_NOTIFY_MANAGER)
                .message(notifyMgrMsg)
                .build());

        return StaffingResponseDetailDto.of(request, response);
    }

    // 3. 사장님 - 수락/거절 최종 승인
    @Transactional
    public StaffingManagerApprovalDetailDto managerApproval(Long ownerUserId, Long requestId, StaffingManagerApprovalDto dto) {
        StaffingRequest request = requestRepo.findById(requestId)
                .orElseThrow(() -> new RuntimeException("요청 데이터를 찾을 수 없습니다."));

        if (!request.getOwner().getUser().getId().equals(ownerUserId)) {
            throw new RuntimeException("해당 요청에 대한 승인 권한이 없습니다.");
        }

        StaffingResponse response = responseRepo.findById(dto.getResponseId())
                .orElseThrow(() -> new RuntimeException("응답 데이터를 찾을 수 없습니다."));

        boolean approved = "APPROVE".equalsIgnoreCase(dto.getAction()) || "APPROVED".equalsIgnoreCase(dto.getAction());
        response.setManagerApproval(approved
                ? StaffingResponse.ManagerApproval.APPROVED
                : StaffingResponse.ManagerApproval.REJECTED);
        responseRepo.save(response);

        boolean shiftAssigned = false;

        if (approved) {
            request.setHeadcountFilled(request.getHeadcountFilled() + 1);
            if (request.getHeadcountFilled() >= request.getHeadcountRequested()) {
                request.setStatus(StaffingRequest.Status.FILLED);
            }
            requestRepo.save(request);

            WorkShift baseShift = workShiftRepo.findById(request.getBaseShiftId())
                    .orElseThrow(() -> new RuntimeException("기준 근무(shiftId=" + request.getBaseShiftId() + ")를 찾을 수 없습니다."));

            Schedule schedule = baseShift.getSchedule();

            WorkShift newShift = new WorkShift();
            newShift.setUserStore(response.getCandidate());
            newShift.setSchedule(schedule);
            newShift.setStartDatetime(request.getStartDatetime());
            newShift.setEndDatetime(request.getEndDatetime());
            newShift.setShiftStatus(WorkShift.ShiftStatus.SCHEDULED);
            workShiftRepo.save(newShift);
            shiftAssigned = true;
        }

        String workerMsg = buildWorkerResultMessage(request, response, shiftAssigned);
        notificationRepo.save(Notification.builder()
                .userId(response.getCandidate().getUser().getId())
                .category(Notification.Category.STAFFING)
                .targetType(Notification.TargetType.STAFFING_RESPONSE)
                .targetId(response.getId())
                .staffingRequestId(request.getId())
                .type(approved
                        ? Notification.Type.STAFFING_MANAGER_APPROVED_WORKER
                        : Notification.Type.STAFFING_MANAGER_REJECTED_WORKER)
                .message(workerMsg)
                .build());

        return StaffingManagerApprovalDetailDto.of(request, response, shiftAssigned);
    }

    // ===== Helper =====
    private StaffingResponse.WorkerAction parseWorkerAction(String action) {
        if (action == null) return StaffingResponse.WorkerAction.NONE;
        switch (action.toUpperCase(Locale.ROOT)) {
            case "ACCEPT": return StaffingResponse.WorkerAction.ACCEPT;
            case "REJECT": return StaffingResponse.WorkerAction.REJECT;
            default: return StaffingResponse.WorkerAction.NONE;
        }
    }

    private StaffingRequestDetailDto toRequestDetailDto(StaffingRequest req) {
        return StaffingRequestDetailDto.builder()
                .requestId(req.getId())
                .storeId(req.getStore().getId())
                .ownerUserId(req.getOwner().getUser().getId())
                .baseShiftId(req.getBaseShiftId())
                .start(req.getStartDatetime().toString())
                .end(req.getEndDatetime().toString())
                .headcountRequested(req.getHeadcountRequested())
                .headcountFilled(req.getHeadcountFilled())
                .status(req.getStatus().name())
                .note(req.getNote())
                .receiverUserIds(csvToLongList(req.getReceiverUserIds()))
                .build();
    }

    private List<Long> csvToLongList(String csv) {
        if (csv == null || csv.isBlank()) return List.of();
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .collect(Collectors.toList());
    }

    // ==== 알림 메시지 빌더 ====
    private String buildStaffInviteMessage(StaffingRequest req) {
        String when = req.getStartDatetime().format(DT) + " ~ " + req.getEndDatetime().format(DT);
        String store = req.getStore().getName();
        return String.format("사장님이 %s / %s 에 대해 인력을 요청했습니다.", store, when);
    }

    private String buildManagerNotifyMessage(StaffingRequest req, StaffingResponse resp) {
        String when = req.getStartDatetime().format(DT) + " ~ " + req.getEndDatetime().format(DT);
        String store = req.getStore().getName();
        String worker = resp.getCandidate().getUser().getUsername();
        String actionKo = switch (resp.getWorkerAction()) {
            case ACCEPT -> "수락";
            case REJECT -> "거절";
            default -> "응답";
        };
        return String.format("%s 님이 %s / %s 에 대한 인력 요청을 %s했습니다.", worker, store, when, actionKo);
    }

    private String buildWorkerResultMessage(StaffingRequest req, StaffingResponse resp, boolean shiftAssigned) {
        String when = req.getStartDatetime().format(DT) + " ~ " + req.getEndDatetime().format(DT);
        String store = req.getStore().getName();
        boolean approved = resp.getManagerApproval() == StaffingResponse.ManagerApproval.APPROVED;
        if (approved) {
            return String.format("사장님이 %s / %s 에 대한 인력 요청을 승인했습니다.%s",
                    store, when, shiftAssigned ? "\n근무가 자동 배정되었습니다." : "");
        } else if (resp.getManagerApproval() == StaffingResponse.ManagerApproval.REJECTED) {
            return String.format("사장님이 %s / %s 에 대한 인력 요청을 거절했습니다.", store, when);
        } else {
            return String.format("사장님 승인 대기 중입니다. (%s / %s)", store, when);
        }
    }
}
