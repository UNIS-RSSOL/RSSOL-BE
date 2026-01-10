package com.example.unis_rssol.schedule.notification;

import com.example.unis_rssol.domain.store.entity.Store;
import com.example.unis_rssol.domain.store.entity.UserStore;
import com.example.unis_rssol.domain.store.repository.StoreRepository;
import com.example.unis_rssol.domain.store.repository.UserStoreRepository;
import com.example.unis_rssol.schedule.notification.dto.NotificationResponseDto;
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

    @Transactional
    public void sendScheduleInputRequest(Long storeId,  LocalDate startDate, LocalDate endDate) {

        List<UserStore> userStores = userStoreRepository.findByStore_Id(storeId);
        String periodText = formatPeriod(startDate, endDate);

        for (UserStore us : userStores) {

            // 사장은 제외
            if (us.getPosition() == UserStore.Position.OWNER) continue;

            Notification notification = Notification.builder()
                    .userId(us.getUser().getId())
                    .storeId(storeId)

                    .category(Notification.Category.SCHEDULE_INPUT)
                    .type(Notification.Type.SCHEDULE_INPUT_REQUEST)

                    // 딥링크용
                    .targetType(null) // or TargetType.SCHEDULE_INPUT_REQUEST
                    .targetId(null)

                    .message(
                            " 사장님이 "+  periodText + " 근무표 입력을 요청했어요! \n" +
                                    "근무 가능한 시간을 기입해주세요!"
                    )
                    .isRead(false)
                    .build();

            notificationRepository.save(notification);
        }
    }

    private String formatPeriod(LocalDate startDate, LocalDate endDate) {
        return startDate.getMonthValue() + "/" + startDate.getDayOfMonth()
                + "-"
                + endDate.getMonthValue() + "/" + endDate.getDayOfMonth();
    }

    // 알림 조회하기
    @Transactional(readOnly = true)
    public List<NotificationResponseDto> getNotifications(Long userId) {
        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        List<NotificationResponseDto> dtos = new ArrayList<>();
        for (Notification n : notifications) {
            NotificationResponseDto dto = NotificationResponseDto.builder()
                    .storeName(storeRepository.findById(n.getStoreId()).map(Store::getName).orElse("Unknown"))
                    .category(n.getCategory())
                    .type(n.getType())
                    .message(n.getMessage())
                    .createdAt(n.getCreatedAt())
                    .isRead(n.isRead())
                    .build();

            dtos.add(dto);

        }
        return dtos;
    }
}
