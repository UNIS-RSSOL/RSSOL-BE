package com.example.unis_rssol.schedule.extrashift.service;

import com.example.unis_rssol.schedule.generation.entity.Schedule;
import com.example.unis_rssol.schedule.generation.entity.WorkShift;
import com.example.unis_rssol.schedule.repository.ScheduleRepository;
import com.example.unis_rssol.schedule.repository.WorkShiftRepository;
import com.example.unis_rssol.schedule.notification.Notification;
import com.example.unis_rssol.schedule.notification.NotificationRepository;
import com.example.unis_rssol.schedule.extrashift.dto.*;
import com.example.unis_rssol.schedule.extrashift.entity.ExtrashiftRequest;
import com.example.unis_rssol.schedule.extrashift.entity.ExtrashiftResponse;
import com.example.unis_rssol.schedule.extrashift.repository.ExtrashiftRequestRepository;
import com.example.unis_rssol.schedule.extrashift.repository.ExtrashiftResponseRepository;
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
public class ExtrashiftService {

    private final ExtrashiftRequestRepository requestRepo;
    private final ExtrashiftResponseRepository responseRepo;
    private final WorkShiftRepository workShiftRepo;
    private final ScheduleRepository scheduleRepo;
    private final UserStoreRepository userStoreRepo;
    private final NotificationRepository notificationRepo;

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // 1. 사장님 추가 인력 요청
    @Transactional
    public ExtrashiftRequestDetailDto create(Long ownerUserId, ExtrashiftCreateDto dto) {
        UserStore ownerStore = userStoreRepo.findByUser_Id(ownerUserId).stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("사장님의 소속 매장을 찾을 수 없습니다."));

        WorkShift baseShift = workShiftRepo.findById(dto.getShiftId())
                .orElseThrow(() -> new RuntimeException("기준 근무를 찾을 수 없습니다."));

        List<UserStore> allStaff = userStoreRepo.findByStore_IdAndPosition(
                ownerStore.getStore().getId(), Position.STAFF
        );

        List<Long> receiverUserIds = allStaff.stream()
                .filter(staff -> !workShiftRepo.existsByUserStore_IdAndStartDatetimeLessThanAndEndDatetimeGreaterThan(
                        staff.getId(),
                        baseShift.getEndDatetime(),
                        baseShift.getStartDatetime()
                ))
                .map(s -> s.getUser().getId())
                .collect(Collectors.toList());

        ExtrashiftRequest request = ExtrashiftRequest.builder()
                .store(ownerStore.getStore())
                .owner(ownerStore)
                .baseShiftId(baseShift.getId())
                .startDatetime(baseShift.getStartDatetime())
                .endDatetime(baseShift.getEndDatetime())
                .headcountRequested(dto.getHeadcount())
                .headcountFilled(0)
                .status(ExtrashiftRequest.Status.OPEN)
                .note(dto.getNote())
                .receiverUserIds(receiverUserIds.stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(",")))
                .build();

        requestRepo.save(request);

        // 알바 초대 알림
        String inviteMsg = buildStaffInviteMessage(request);
        for (Long receiverId : receiverUserIds) {
            notificationRepo.save(Notification.builder()
                    .userId(receiverId)
                    .store(request.getStore())
                    .category(Notification.Category.EXTRA_SHIFT)
                    .targetType(Notification.TargetType.EXTRA_SHIFT_REQUEST)
                    .targetId(request.getId())
                    .extraShiftRequestId(request.getId())
                    .type(Notification.Type.EXTRA_SHIFT_REQUEST_INVITE)
                    .message(inviteMsg)
                    .build());
        }

        return toRequestDetailDto(request);
    }

    // 2. 알바 응답 (수락 / 거절)
    @Transactional
    public ExtrashiftResponseDetailDto respond(Long userId, Long requestId, ExtrashiftRespondDto dto) {
        ExtrashiftRequest request = requestRepo.findById(requestId)
                .orElseThrow(() -> new RuntimeException("인력 요청을 찾을 수 없습니다."));

        UserStore candidate = userStoreRepo.findByUser_Id(userId).stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("소속 매장을 찾을 수 없습니다."));

        if (responseRepo.existsByExtraShiftRequest_IdAndCandidate_Id(requestId, candidate.getId())) {
            throw new RuntimeException("이미 응답한 요청입니다.");
        }

        ExtrashiftResponse response = ExtrashiftResponse.builder()
                .extraShiftRequest(request)
                .candidate(candidate)
                .workerAction(parseWorkerAction(dto.getAction()))
                .managerApproval(ExtrashiftResponse.ManagerApproval.PENDING)
                .build();

        responseRepo.save(response);

        // 사장 알림
        String notifyMgrMsg = buildManagerNotifyMessage(request, response);
        notificationRepo.save(Notification.builder()
                .userId(request.getOwner().getUser().getId())
                .store(request.getStore())
                .category(Notification.Category.EXTRA_SHIFT)
                .targetType(Notification.TargetType.EXTRA_SHIFT_RESPONSE)
                .targetId(response.getId())
                .extraShiftRequestId(request.getId())
                .type(Notification.Type.EXTRA_SHIFT_NOTIFY_MANAGER)
                .message(notifyMgrMsg)
                .build());

        return ExtrashiftResponseDetailDto.of(request, response);
    }

    // 3. 사장 최종 승인 / 거절
    @Transactional
    public ExtrashiftManagerApprovalDetailDto managerApproval(
            Long ownerUserId, Long requestId, ExtrashiftManagerApprovalDto dto) {

        ExtrashiftRequest request = requestRepo.findById(requestId)
                .orElseThrow(() -> new RuntimeException("요청을 찾을 수 없습니다."));

        if (!request.getOwner().getUser().getId().equals(ownerUserId)) {
            throw new RuntimeException("승인 권한이 없습니다.");
        }

        ExtrashiftResponse response = responseRepo.findById(dto.getResponseId())
                .orElseThrow(() -> new RuntimeException("응답 데이터를 찾을 수 없습니다."));

        boolean approved = "APPROVE".equalsIgnoreCase(dto.getAction())
                || "APPROVED".equalsIgnoreCase(dto.getAction());

        response.setManagerApproval(approved
                ? ExtrashiftResponse.ManagerApproval.APPROVED
                : ExtrashiftResponse.ManagerApproval.REJECTED);
        responseRepo.save(response);

        boolean shiftAssigned = false;

        if (approved) {
            request.setHeadcountFilled(request.getHeadcountFilled() + 1);
            if (request.getHeadcountFilled() >= request.getHeadcountRequested()) {
                request.setStatus(ExtrashiftRequest.Status.FILLED);
            }
            requestRepo.save(request);

            WorkShift baseShift = workShiftRepo.findById(request.getBaseShiftId())
                    .orElseThrow(() -> new RuntimeException("기준 근무를 찾을 수 없습니다."));

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

        // 알바 결과 알림
        String workerMsg = buildWorkerResultMessage(request, response, shiftAssigned);
        notificationRepo.save(Notification.builder()
                .userId(response.getCandidate().getUser().getId())
                .store(request.getStore())
                .category(Notification.Category.EXTRA_SHIFT)
                .targetType(Notification.TargetType.EXTRA_SHIFT_RESPONSE)
                .targetId(response.getId())
                .extraShiftRequestId(request.getId())
                .type(approved
                        ? Notification.Type.EXTRA_SHIFT_MANAGER_APPROVED_WORKER
                        : Notification.Type.EXTRA_SHIFT_MANAGER_REJECTED_WORKER)
                .message(workerMsg)
                .build());

        return ExtrashiftManagerApprovalDetailDto.of(request, response, shiftAssigned);
    }

    // ===== Helper =====
    private ExtrashiftResponse.WorkerAction parseWorkerAction(String action) {
        if (action == null) return ExtrashiftResponse.WorkerAction.NONE;
        return switch (action.toUpperCase(Locale.ROOT)) {
            case "ACCEPT" -> ExtrashiftResponse.WorkerAction.ACCEPT;
            case "REJECT" -> ExtrashiftResponse.WorkerAction.REJECT;
            default -> ExtrashiftResponse.WorkerAction.NONE;
        };
    }

    private ExtrashiftRequestDetailDto toRequestDetailDto(ExtrashiftRequest req) {
        return ExtrashiftRequestDetailDto.builder()
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

    // ===== 알림 메시지 =====
    private String buildStaffInviteMessage(ExtrashiftRequest req) {
        String when = req.getStartDatetime().format(DT) + " ~ " + req.getEndDatetime().format(DT);
        return String.format("사장님이 %s / %s 에 대해 인력을 요청했습니다.",
                req.getStore().getName(), when);
    }

    private String buildManagerNotifyMessage(ExtrashiftRequest req, ExtrashiftResponse resp) {
        String when = req.getStartDatetime().format(DT) + " ~ " + req.getEndDatetime().format(DT);
        String actionKo = switch (resp.getWorkerAction()) {
            case ACCEPT -> "수락";
            case REJECT -> "거절";
            default -> "응답";
        };
        return String.format("%s 님이 %s / %s 인력 요청을 %s했습니다.",
                resp.getCandidate().getUser().getUsername(),
                req.getStore().getName(), when, actionKo);
    }

    private String buildWorkerResultMessage(
            ExtrashiftRequest req, ExtrashiftResponse resp, boolean shiftAssigned) {

        String when = req.getStartDatetime().format(DT) + " ~ " + req.getEndDatetime().format(DT);
        if (resp.getManagerApproval() == ExtrashiftResponse.ManagerApproval.APPROVED) {
            return String.format("사장님이 %s / %s 인력 요청을 승인했습니다.%s",
                    req.getStore().getName(), when,
                    shiftAssigned ? "\n근무가 자동 배정되었습니다." : "");
        } else {
            return String.format("사장님이 %s / %s 인력 요청을 거절했습니다.",
                    req.getStore().getName(), when);
        }
    }
}
