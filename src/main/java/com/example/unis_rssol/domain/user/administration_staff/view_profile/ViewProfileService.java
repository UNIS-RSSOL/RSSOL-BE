package com.example.unis_rssol.domain.user.administration_staff.view_profile;

import com.example.unis_rssol.domain.bank.BankAccount;
import com.example.unis_rssol.domain.bank.BankAccountRepository;
import com.example.unis_rssol.domain.store.UserStore;
import com.example.unis_rssol.domain.store.UserStoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class ViewProfileService {

    private final UserStoreRepository userStoreRepository;
    private final BankAccountRepository bankAccountRepository;

    @Transactional(readOnly = true)
    public ViewProfileResponse getEmployeeProfile(
            Long ownerId,
            Long userStoreId
    ) {

        UserStore userStore = userStoreRepository.findById(userStoreId)
                .orElseThrow(() -> new IllegalArgumentException("USER_STORE_NOT_FOUND"));

        boolean isOwner = userStoreRepository
                .findByUser_IdAndStore_Id(ownerId, userStore.getStore().getId())
                .stream()
                .anyMatch(us -> us.getPosition() == UserStore.Position.OWNER);

        if (!isOwner) {
            throw new IllegalArgumentException("ACCESS_DENIED");
        }

        var user = userStore.getUser();
        var store = userStore.getStore();

        BankAccount bankAccount =
                bankAccountRepository.findTopByUserIdOrderByIdDesc(user.getId())
                        .orElse(null);

        LocalDate hireDate = userStore.getHireDate();

        long daysWorked = hireDate != null
                ? ChronoUnit.DAYS.between(hireDate, LocalDate.now())
                : 0;

        return new ViewProfileResponse(
                user.getUsername(),
                user.getProfileImageUrl(),
                userStore.getEmploymentStatus().name(),
                userStore.getPosition().name(),
                store.getName(),
                bankAccount != null && bankAccount.getBank() != null
                        ? bankAccount.getBank().getBankName()
                        : null,
                bankAccount != null ? bankAccount.getAccountNumber() : null,
                user.getEmail(),
                hireDate,
                daysWorked
        );
    }
}