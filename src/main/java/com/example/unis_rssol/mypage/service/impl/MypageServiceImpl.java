package com.example.unis_rssol.mypage.service.impl;

import com.example.unis_rssol.domain.bank.entity.Bank;
import com.example.unis_rssol.domain.bank.entity.BankAccount;
import com.example.unis_rssol.domain.bank.repository.BankAccountRepository;
import com.example.unis_rssol.domain.bank.repository.BankRepository;
import com.example.unis_rssol.global.fordevToken.StoreCodeGenerator;
import com.example.unis_rssol.mypage.dto.*;
import com.example.unis_rssol.mypage.service.MypageService;
import com.example.unis_rssol.domain.store.entity.Store;
import com.example.unis_rssol.domain.store.entity.UserStore;
import com.example.unis_rssol.domain.store.entity.UserStore.EmploymentStatus;
import com.example.unis_rssol.domain.store.entity.UserStore.Position;
import com.example.unis_rssol.domain.store.repository.StoreRepository;
import com.example.unis_rssol.domain.store.repository.UserStoreRepository;
import com.example.unis_rssol.domain.user.entity.AppUser;
import com.example.unis_rssol.domain.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class MypageServiceImpl implements MypageService {

    private final AppUserRepository users;
    private final StoreRepository stores;
    private final UserStoreRepository userStores;
    private final BankRepository banks;
    private final BankAccountRepository accounts;

    // 내부 헬퍼

    private UserStore ensureMapping(Long userId, Long storeId) {
        return userStores.findByUserIdAndStoreId(userId, storeId)
                .orElseThrow(() -> new IllegalArgumentException("해당 매장에 속하지 않은 사용자입니다."));
    }

    private UserStore resolveActiveMappingOrDefault(Long userId) {
        AppUser u = users.findById(userId).orElseThrow();

        if (u.getActiveStoreId() != null) {
            return ensureMapping(userId, u.getActiveStoreId());
        }

        return userStores.findFirstByUserIdOrderByCreatedAtAsc(userId)
                .orElseThrow(() -> new IllegalArgumentException("등록된 매장이 없습니다."));
    }

    private ActiveStoreResponse toActiveStoreResponse(UserStore mapping) {
        Store s = mapping.getStore();
        return ActiveStoreResponse.builder()
                .storeId(s.getId())
                .storeCode(s.getStoreCode())
                .name(s.getName())
                .address(s.getAddress())
                .phoneNumber(s.getPhoneNumber())
                .businessRegistrationNumber(s.getBusinessRegistrationNumber())
                .position(mapping.getPosition().name())
                .employmentStatus(mapping.getEmploymentStatus().name())
                .build();
    }

    private StoreSimpleResponse toStoreSimple(UserStore mapping, boolean includeStatus) {
        Store s = mapping.getStore();
        return StoreSimpleResponse.builder()
                .storeId(s.getId())
                .storeCode(s.getStoreCode())
                .name(s.getName())
                .address(s.getAddress())
                .phoneNumber(s.getPhoneNumber())
                .businessRegistrationNumber(s.getBusinessRegistrationNumber())
                .position(mapping.getPosition().name())
                .employmentStatus(includeStatus ? mapping.getEmploymentStatus().name() : null)
                .build();
    }

    // 활성 매장

    @Override
    @Transactional(readOnly = true)
    public ActiveStoreResponse getActiveStore(Long userId) {
        UserStore mapping = resolveActiveMappingOrDefault(userId);
        return toActiveStoreResponse(mapping);
    }

    @Override
    public ActiveStoreResponse updateActiveStore(Long userId, Long storeId) {
        AppUser u = users.findById(userId).orElseThrow();
        ensureMapping(userId, storeId);
        u.setActiveStoreId(storeId);
        users.save(u);

        return toActiveStoreResponse(ensureMapping(userId, storeId));
    }

    // 사장님 관련

    @Override
    @Transactional(readOnly = true)
    public OwnerProfileResponse getOwnerProfile(Long ownerId) {
        UserStore mapping = resolveActiveMappingOrDefault(ownerId);
        if (mapping.getPosition() != Position.OWNER) {
            throw new IllegalArgumentException("사장님 권한이 필요한 요청입니다.");
        }
        AppUser u = mapping.getUser();
        Store s = mapping.getStore();

        return OwnerProfileResponse.builder()
                .userId(u.getId())
                .username(u.getUsername())
                .email(u.getEmail())
                .profileImageUrl(u.getProfileImageUrl())
                .position(mapping.getPosition().name())
                .employmentStatus(mapping.getEmploymentStatus().name())
                .businessRegistrationNumber(s.getBusinessRegistrationNumber())
                .build();
    }

    @Override
    public OwnerProfileResponse updateOwnerProfile(Long ownerId, OwnerProfileUpdateRequest req) {
        UserStore mapping = resolveActiveMappingOrDefault(ownerId);
        if (mapping.getPosition() != Position.OWNER) {
            throw new IllegalArgumentException("사장님 권한이 필요한 요청입니다.");
        }
        AppUser u = mapping.getUser();
        Store s = mapping.getStore();

        if (req.getUsername() != null) u.setUsername(req.getUsername());
        if (req.getEmail() != null) u.setEmail(req.getEmail());
        users.save(u);

        if (req.getBusinessRegistrationNumber() != null
                && !req.getBusinessRegistrationNumber().equals(s.getBusinessRegistrationNumber())) {
            if (stores.existsByBusinessRegistrationNumber(req.getBusinessRegistrationNumber())) {
                throw new IllegalArgumentException("이미 사용 중인 사업자 등록번호입니다.");
            }
            s.setBusinessRegistrationNumber(req.getBusinessRegistrationNumber());
            stores.save(s);
        }

        return getOwnerProfile(ownerId);
    }

    @Override
    @Transactional(readOnly = true)
    public OwnerStoreResponse getOwnerActiveStore(Long ownerId) {
        UserStore mapping = resolveActiveMappingOrDefault(ownerId);
        if (mapping.getPosition() != Position.OWNER) {
            throw new IllegalArgumentException("사장님 권한이 필요한 요청입니다.");
        }
        Store s = mapping.getStore();

        return OwnerStoreResponse.builder()
                .storeId(s.getId())
                .storeCode(s.getStoreCode())
                .name(s.getName())
                .address(s.getAddress())
                .phoneNumber(s.getPhoneNumber())
                // .businessRegistrationNumber(s.getBusinessRegistrationNumber())
                .build();
    }

    @Override
    public OwnerStoreResponse updateOwnerActiveStore(Long ownerId, OwnerStoreUpdateRequest req) {
        UserStore mapping = resolveActiveMappingOrDefault(ownerId);
        if (mapping.getPosition() != Position.OWNER) {
            throw new IllegalArgumentException("사장님 권한이 필요한 요청입니다.");
        }
        Store s = mapping.getStore();

        if (req.getName() != null) s.setName(req.getName());
        if (req.getAddress() != null) s.setAddress(req.getAddress());
        if (req.getPhoneNumber() != null) s.setPhoneNumber(req.getPhoneNumber());

//        if (req.getBusinessRegistrationNumber() != null
//                && !req.getBusinessRegistrationNumber().equals(s.getBusinessRegistrationNumber())) {
//            if (stores.existsByBusinessRegistrationNumber(req.getBusinessRegistrationNumber())) {
//                throw new IllegalArgumentException("이미 사용 중인 사업자 등록번호입니다.");
//            }
//            s.setBusinessRegistrationNumber(req.getBusinessRegistrationNumber());
//        }

        stores.save(s);
        return getOwnerActiveStore(ownerId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoreSimpleResponse> listOwnerStores(Long ownerId) {
        return userStores.findByUserIdAndPosition(ownerId, Position.OWNER)
                .stream()
                .sorted(Comparator.comparing(us -> us.getStore().getId()))
                .map(us -> toStoreSimple(us, true))
                .toList();
    }

    @Override
    public StoreSimpleResponse addOwnerStore(Long ownerId, OwnerCreateStoreRequest req) {
        AppUser owner = users.findById(ownerId).orElseThrow();

        Store store = Store.builder()
                .storeCode(StoreCodeGenerator.generate())
                .name(req.getName())
                .address(req.getAddress())
                .phoneNumber(req.getPhoneNumber())
                .businessRegistrationNumber(req.getBusinessRegistrationNumber())
                .build();
        stores.save(store);

        UserStore link = UserStore.builder()
                .user(owner).store(store)
                .position(Position.OWNER)
                .employmentStatus(EmploymentStatus.HIRED)
                .hireDate(req.getHireDate())
                .build();
        userStores.save(link);

        return StoreSimpleResponse.builder()
                .storeId(store.getId())
                .storeCode(store.getStoreCode())
                .name(store.getName())
                .address(store.getAddress())
                .phoneNumber(store.getPhoneNumber())
                .businessRegistrationNumber(store.getBusinessRegistrationNumber())
                .position("OWNER")
                .employmentStatus("HIRED")
                .hireDate(link.getHireDate())
                .build();
    }

    @Override
    public void removeOwnerStore(Long ownerId, Long storeId) {
        UserStore mapping = ensureMapping(ownerId, storeId);
        if (mapping.getPosition() != Position.OWNER) {
            throw new IllegalArgumentException("사장님 권한이 필요한 요청입니다.");
        }
        userStores.delete(mapping);

        AppUser u = users.findById(ownerId).orElseThrow();
        if (storeId.equals(u.getActiveStoreId())) {
            Long nextActive = userStores.findByUserId(ownerId).stream()
                    .findFirst()
                    .map(us -> us.getStore().getId())
                    .orElse(null);
            u.setActiveStoreId(nextActive);
            users.save(u);
        }
    }

    // 알바생 관련

    @Override
    @Transactional(readOnly = true)
    public StaffProfileResponse getStaffProfile(Long staffId) {
        UserStore mapping = resolveActiveMappingOrDefault(staffId);
        if (mapping.getPosition() != Position.STAFF) {
            throw new IllegalArgumentException("알바생 권한이 필요한 요청입니다.");
        }

        AppUser u = mapping.getUser();
        Store s = mapping.getStore();

        BankAccount latest = accounts.findTopByUserIdOrderByIdDesc(staffId).orElse(null);

        return StaffProfileResponse.builder()
                .userId(u.getId())
                .username(u.getUsername())
                .email(u.getEmail())
                .profileImageUrl(u.getProfileImageUrl())
                .position(mapping.getPosition().name())
                .employmentStatus(mapping.getEmploymentStatus().name())
                .currentStore(StaffProfileResponse.CurrentStore.builder()
                        .storeId(s.getId())
                        .name(s.getName())
                        .storeCode(s.getStoreCode())
                        .build())
                .bankAccount(latest == null ? null :
                        StaffProfileResponse.BankAccount.builder()
                                .bankId(latest.getBank().getId())
                                .bankName(latest.getBank().getBankName())
                                .accountNumber(latest.getAccountNumber())
                                .build())
                .build();
    }

    @Override
    public StaffProfileResponse updateStaffProfile(Long staffId, StaffProfileUpdateRequest req) {
        UserStore mapping = resolveActiveMappingOrDefault(staffId);
        if (mapping.getPosition() != Position.STAFF) {
            throw new IllegalArgumentException("알바생 권한이 필요한 요청입니다.");
        }
        AppUser u = mapping.getUser();

        if (req.getUsername() != null) u.setUsername(req.getUsername());
        if (req.getEmail() != null) u.setEmail(req.getEmail());
        users.save(u);

        if (req.getBankId() != null || req.getAccountNumber() != null) {
            BankAccount latest = accounts.findTopByUserIdOrderByIdDesc(staffId).orElse(null);

            if (latest == null) {
                if (req.getBankId() != null && req.getAccountNumber() != null) {
                    Bank bank = banks.findById(req.getBankId())
                            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 은행입니다."));
                    accounts.save(BankAccount.builder()
                            .user(u)
                            .bank(bank)
                            .accountNumber(req.getAccountNumber())
                            .build());
                }
            } else {
                if (req.getBankId() != null) {
                    Bank bank = banks.findById(req.getBankId())
                            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 은행입니다."));
                    latest.setBank(bank);
                }
                if (req.getAccountNumber() != null) latest.setAccountNumber(req.getAccountNumber());
                accounts.save(latest);
            }
        }

        return getStaffProfile(staffId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoreSimpleResponse> listStaffStores(Long staffId) {
        return userStores.findByUserIdAndPosition(staffId, Position.STAFF)
                .stream()
                .sorted(Comparator.comparing(us -> us.getStore().getId()))
                .map(us -> toStoreSimple(us, true))
                .toList();
    }

    @Override
    public StoreSimpleResponse joinStaffStore(Long staffId, StaffJoinStoreRequest req) {
        AppUser staff = users.findById(staffId).orElseThrow();
        Store store = stores.findByStoreCode(req.getStoreCode())
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 매장 코드입니다."));

        if (userStores.existsByUserIdAndStoreId(staffId, store.getId())) {
            throw new IllegalArgumentException("이미 등록된 매장입니다.");
        }

        UserStore link = UserStore.builder()
                .user(staff).store(store)
                .position(Position.STAFF)
                .employmentStatus(EmploymentStatus.HIRED)
                .hireDate(req.getHireDate())
                .build();
        userStores.save(link);

        return StoreSimpleResponse.builder()
                .storeId(store.getId())
                .storeCode(store.getStoreCode())
                .name(store.getName())
                .address(store.getAddress())
                .phoneNumber(store.getPhoneNumber())
                .businessRegistrationNumber(store.getBusinessRegistrationNumber())
                .position("STAFF")
                .employmentStatus("HIRED")
                .hireDate(link.getHireDate())
                .build();
    }

    @Override
    public void leaveStaffStore(Long staffId, Long storeId) {
        UserStore mapping = ensureMapping(staffId, storeId);
        if (mapping.getPosition() != Position.STAFF) {
            throw new IllegalArgumentException("알바생 권한이 필요한 요청입니다.");
        }
        userStores.delete(mapping);

        AppUser u = users.findById(staffId).orElseThrow();
        if (storeId.equals(u.getActiveStoreId())) {
            Long nextActive = userStores.findByUserId(staffId).stream()
                    .findFirst()
                    .map(us -> us.getStore().getId())
                    .orElse(null);
            u.setActiveStoreId(nextActive);
            users.save(u);
        }
    }
}
