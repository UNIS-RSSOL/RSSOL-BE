package com.example.unis_rssol.schedule.notification;

import com.example.unis_rssol.domain.store.entity.Store;
import com.example.unis_rssol.domain.store.entity.UserStore;
import com.example.unis_rssol.domain.store.repository.StoreRepository;
import com.example.unis_rssol.domain.store.repository.UserStoreRepository;
import com.example.unis_rssol.domain.user.entity.AppUser;
import com.example.unis_rssol.domain.user.repository.AppUserRepository;
import com.example.unis_rssol.schedule.extrashift.entity.ExtrashiftRequest;
import com.example.unis_rssol.schedule.extrashift.repository.ExtrashiftRequestRepository;
import com.example.unis_rssol.schedule.notification.dto.NotificationResponseDto;
import com.example.unis_rssol.schedule.shiftswap.entity.ShiftSwapRequest;
import com.example.unis_rssol.schedule.shiftswap.repository.ShiftSwapRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final UserStoreRepository userStoreRepository;
    private final NotificationRepository notificationRepository;
    private final StoreRepository storeRepository;
    private final AppUserRepository userRepository;
    private final ShiftSwapRequestRepository shiftSwapRequestRepository;
    private final ExtrashiftRequestRepository extrashiftRequestRepository;

    // 근무표 입력 요청 알림
    @Transactional
    public void sendScheduleInputRequest(Long storeId, LocalDate startDate, LocalDate endDate) {

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("매장을 찾을 수 없습니다."));

        List<UserStore> userStores = userStoreRepository.findByStore_Id(storeId);
        String periodText = formatPeriod(startDate, endDate);

        for (UserStore us : userStores) {
            if (us.getPosition() == UserStore.Position.OWNER) continue;

            Notification notification = Notification.builder()
                    .userId(us.getUser().getId())
                    .store(store)
                    .category(Notification.Category.SCHEDULE_INPUT)
                    .type(Notification.Type.SCHEDULE_INPUT_REQUEST)
                    .message(
                            "사장님이 " + periodText + " 근무표 입력을 요청했어요.\n" +
                                    "근무 가능한 시간을 기입해주세요!"
                    )
                    .isRead(false)
                    .build();

            notificationRepository.save(notification);
        }
    }

    private String formatPeriod(LocalDate startDate, LocalDate endDate) {
        return startDate.getMonthValue() + "/" + startDate.getDayOfMonth()
                + "-" +
                endDate.getMonthValue() + "/" + endDate.getDayOfMonth();
    }

    // 알림 조회 (status 포함)
    @Transactional(readOnly = true)
    public List<NotificationResponseDto> getNotifications(Long userId) {

        AppUser user = userRepository.findById(userId).orElseThrow();
        List<Notification> notifications = notificationRepository.findByUserIdWithStore(userId);

        List<NotificationResponseDto> dtos = new ArrayList<>();

        for (Notification n : notifications) {

            ShiftSwapRequest shiftSwap = null;
            ExtrashiftRequest extraShift = null;

            if (n.getShiftSwapRequestId() != null) {
                shiftSwap = shiftSwapRequestRepository
                        .findById(n.getShiftSwapRequestId())
                        .orElse(null);
            }

            if (n.getExtraShiftRequestId() != null) {
                extraShift = extrashiftRequestRepository
                        .findById(n.getExtraShiftRequestId())
                        .orElse(null);
            }

            NotificationResponseDto dto = NotificationResponseDto.builder()
                    .profileImageUrl(user.getProfileImageUrl())
                    .storeName(n.getStore() != null ? n.getStore().getName() : null)
                    .category(n.getCategory())
                    .type(n.getType())
                    .message(n.getMessage())
                    .createdAt(n.getCreatedAt())

                    // 요청 id 추가
                    .shiftSwapRequestId(n.getShiftSwapRequestId())
                    .extraShiftRequestId(n.getExtraShiftRequestId())

                    // 요청 상태 status 추가
                    .shiftSwapStatus(shiftSwap != null ? shiftSwap.getStatus() : null)
                    .shiftSwapManagerApprovalStatus(
                            shiftSwap != null ? shiftSwap.getManagerApprovalStatus() : null
                    )
                    .extraShiftStatus(extraShift != null ? extraShift.getStatus() : null)

                    .isRead(n.isRead())
                    .build();

            dtos.add(dto);
        }

        return dtos;
    }
}
