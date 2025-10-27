package com.example.unis_rssol.shift.service;

import com.example.unis_rssol.schedule.entity.WorkShift;
import com.example.unis_rssol.schedule.repository.WorkShiftRepository;
import com.example.unis_rssol.shift.dto.ShiftSwapManagerApprovalDto;
import com.example.unis_rssol.shift.dto.ShiftSwapRequestCreateDto;
import com.example.unis_rssol.shift.dto.ShiftSwapRespondDto;
import com.example.unis_rssol.shift.dto.ShiftSwapResponseDto;
import com.example.unis_rssol.shift.entity.Notification;
import com.example.unis_rssol.shift.entity.ShiftSwapRequest;
import com.example.unis_rssol.shift.repository.NotificationRepository;
import com.example.unis_rssol.shift.repository.ShiftSwapRequestRepository;
import com.example.unis_rssol.store.entity.UserStore;
import com.example.unis_rssol.store.repository.UserStoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShiftSwapService {

    private final ShiftSwapRequestRepository requestRepo;
    private final NotificationRepository notificationRepo;
    private final WorkShiftRepository workShiftRepo;
    private final UserStoreRepository userStoreRepo;

    // ① 대타 요청 생성 (후보 전원 → 배열 응답)
    @Transactional
    public List<ShiftSwapResponseDto> create(Long userId, ShiftSwapRequestCreateDto dto) {
        WorkShift shift = workShiftRepo.findById(dto.getShiftId())
                .orElseThrow(() -> new RuntimeException("해당 근무를 찾을 수 없습니다."));

        // 요청자 검증: 본인 근무만 가능
        UserStore requester = userStoreRepo.findByUser_IdAndStore_Id(
                userId,
                shift.getUserStore().getStore().getId()
        ).orElseThrow(() -> new RuntimeException("요청자 정보가 없거나, 해당 매장 소속이 아닙니다."));

        if (!shift.getUserStore().getId().equals(requester.getId())) {
            throw new RuntimeException("본인 근무에 대해서만 대타 요청을 생성할 수 있습니다.");
        }

        var storeId = shift.getUserStore().getStore().getId();
        var start   = shift.getStartDatetime();
        var end     = shift.getEndDatetime();

        // 후보 선정
        List<UserStore> candidates = userStoreRepo.findByStore_Id(storeId).stream()
                .filter(u -> !u.getId().equals(requester.getId())) // 1) 본인 제외
                // .filter(u -> u.getPosition() == UserStore.Position.STAFF) // (옵션) 사장 제외
                // 2) '조금이라도' 겹치면 제외
                .filter(u -> !workShiftRepo.existsByUserStore_IdAndStartDatetimeLessThanAndEndDatetimeGreaterThan(
                        u.getId(), end, start))
                .toList();

        // (권장) 중복 방지: 같은 수신자에게 진행 중(PENDING/ACCEPTED) 요청이 있으면 스킵
        var dupStatuses = List.of(ShiftSwapRequest.Status.PENDING, ShiftSwapRequest.Status.ACCEPTED);

        List<ShiftSwapResponseDto> results = new ArrayList<>();
        for (UserStore receiver : candidates) {

            if (requestRepo.existsByShift_IdAndReceiver_IdAndStatusIn(
                    shift.getId(), receiver.getId(), dupStatuses)) {
                continue; // 이미 진행 중이면 패스
            }

            ShiftSwapRequest request = requestRepo.save(ShiftSwapRequest.builder()
                    .shift(shift)
                    .requester(requester)
                    .receiver(receiver)
                    .reason(dto.getReason())
                    .status(ShiftSwapRequest.Status.PENDING)
                    .managerApprovalStatus(ShiftSwapRequest.ManagerApproval.PENDING)
                    .build());

            notificationRepo.save(Notification.builder()
                    .userId(receiver.getUser().getId())
                    .shiftSwapRequestId(request.getId())
                    .type(Notification.Type.SHIFT_SWAP_REQUEST)
                    .message(requester.getUser().getUsername() + "님이 "
                            + start.toLocalDate() + " " + start.toLocalTime()
                            + " 근무 대타를 요청했습니다.")
                    .build());

            results.add(ShiftSwapResponseDto.from(request));
        }

        return results;
    }

    // ② 수신자 응답
    @Transactional
    public ShiftSwapResponseDto respond(Long userId, Long requestId, ShiftSwapRespondDto dto) {
        ShiftSwapRequest request = requestRepo.findById(requestId)
                .orElseThrow(() -> new RuntimeException("대타 요청을 찾을 수 없습니다."));

        if (!request.getReceiver().getUser().getId().equals(userId)) {
            throw new RuntimeException("이 대타 요청에 응답할 권한이 없습니다.");
        }

        String action = dto.getAction() == null ? "" : dto.getAction().toUpperCase();

        switch (action) {
            case "REJECT" -> {
                request.setStatus(ShiftSwapRequest.Status.REJECTED);
                notificationRepo.save(Notification.builder()
                        .userId(request.getRequester().getUser().getId())
                        .shiftSwapRequestId(request.getId())
                        .type(Notification.Type.SHIFT_SWAP_MANAGER_REJECTED_REQUESTER)
                        .message("대타 요청이 거절되었습니다.")
                        .build());
            }
            case "ACCEPT" -> {
                if (request.getReceiver().getPosition() == UserStore.Position.OWNER) {
                    // 사장 → 즉시 승인
                    request.setStatus(ShiftSwapRequest.Status.ACCEPTED);
                    request.setManagerApprovalStatus(ShiftSwapRequest.ManagerApproval.APPROVED);

                    WorkShift shift = request.getShift();
                    shift.setUserStore(request.getReceiver());
                    workShiftRepo.save(shift);

                    notificationRepo.saveAll(List.of(
                            Notification.builder()
                                    .userId(request.getRequester().getUser().getId())
                                    .shiftSwapRequestId(request.getId())
                                    .type(Notification.Type.SHIFT_SWAP_MANAGER_APPROVED_REQUESTER)
                                    .message("사장님이 대타 요청을 최종 승인했습니다.")
                                    .build(),
                            Notification.builder()
                                    .userId(request.getReceiver().getUser().getId())
                                    .shiftSwapRequestId(request.getId())
                                    .type(Notification.Type.SHIFT_SWAP_MANAGER_APPROVED_RECEIVER)
                                    .message("대타 요청이 최종 승인되었습니다.")
                                    .build()
                    ));
                } else {
                    // 알바 → 1차 수락, 사장 승인 요청
                    request.setStatus(ShiftSwapRequest.Status.ACCEPTED);
                    request.setManagerApprovalStatus(ShiftSwapRequest.ManagerApproval.PENDING);

                    List<UserStore> owners = userStoreRepo.findByStoreIdAndPosition(
                            request.getRequester().getStore().getId(),
                            UserStore.Position.OWNER
                    );

                    owners.forEach(owner -> notificationRepo.save(Notification.builder()
                            .userId(owner.getUser().getId())
                            .shiftSwapRequestId(request.getId())
                            .type(Notification.Type.SHIFT_SWAP_NOTIFY_MANAGER)
                            .message(request.getReceiver().getUser().getUsername() +
                                    "님이 " + request.getRequester().getUser().getUsername() +
                                    "님의 대타 요청을 수락했습니다. 최종 승인하시겠어요?")
                            .build()));
                }
            }
            default -> throw new RuntimeException("지원하지 않는 action 입니다. (ACCEPT/REJECT)");
        }

        return ShiftSwapResponseDto.from(request);
    }

    // ③ 사장 최종 승인/거절
    @Transactional
    public ShiftSwapResponseDto managerApproval(Long userId, Long requestId, ShiftSwapManagerApprovalDto dto) {
        ShiftSwapRequest request = requestRepo.findById(requestId)
                .orElseThrow(() -> new RuntimeException("대타 요청을 찾을 수 없습니다."));

        List<UserStore> owners = userStoreRepo.findByStoreIdAndPosition(
                request.getRequester().getStore().getId(), UserStore.Position.OWNER);
        boolean isOwner = owners.stream().anyMatch(o -> o.getUser().getId().equals(userId));
        if (!isOwner) throw new RuntimeException("해당 매장의 사장만 승인/거절할 수 있습니다.");

        String action = dto.getAction() == null ? "" : dto.getAction().toUpperCase();

        switch (action) {
            case "APPROVE" -> {
                request.setManagerApprovalStatus(ShiftSwapRequest.ManagerApproval.APPROVED);
                request.getShift().setUserStore(request.getReceiver());
                workShiftRepo.save(request.getShift());

                notificationRepo.saveAll(List.of(
                        Notification.builder()
                                .userId(request.getRequester().getUser().getId())
                                .shiftSwapRequestId(request.getId())
                                .type(Notification.Type.SHIFT_SWAP_MANAGER_APPROVED_REQUESTER)
                                .message("대타 요청이 최종 승인되었습니다.")
                                .build(),
                        Notification.builder()
                                .userId(request.getReceiver().getUser().getId())
                                .shiftSwapRequestId(request.getId())
                                .type(Notification.Type.SHIFT_SWAP_MANAGER_APPROVED_RECEIVER)
                                .message("당신이 수락한 대타 요청이 최종 승인되었습니다.")
                                .build()
                ));
            }
            case "REJECT" -> {
                request.setManagerApprovalStatus(ShiftSwapRequest.ManagerApproval.REJECTED);
                notificationRepo.saveAll(List.of(
                        Notification.builder()
                                .userId(request.getRequester().getUser().getId())
                                .shiftSwapRequestId(request.getId())
                                .type(Notification.Type.SHIFT_SWAP_MANAGER_REJECTED_REQUESTER)
                                .message("대타 요청이 최종 거절되었습니다.")
                                .build(),
                        Notification.builder()
                                .userId(request.getReceiver().getUser().getId())
                                .shiftSwapRequestId(request.getId())
                                .type(Notification.Type.SHIFT_SWAP_MANAGER_REJECTED_RECEIVER)
                                .message("당신이 수락한 대타 요청이 거절되었습니다.")
                                .build()
                ));
            }
            default -> throw new RuntimeException("지원하지 않는 action 입니다. (APPROVE/REJECT)");
        }

        return ShiftSwapResponseDto.from(request);
    }

    // ④ 알림 조회 (기존)
    @Transactional(readOnly = true)
    public List<Notification> getNotifications(Long userId) {
        return notificationRepo.findByUserIdOrderByCreatedAtDesc(userId);
    }
}
