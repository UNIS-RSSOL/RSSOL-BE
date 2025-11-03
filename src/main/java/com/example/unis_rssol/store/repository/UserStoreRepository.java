package com.example.unis_rssol.store.repository;

import com.example.unis_rssol.store.entity.UserStore;
import com.example.unis_rssol.store.entity.UserStore.Position;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserStoreRepository extends JpaRepository<UserStore, Long> {

    // ====== 표준(Canonical) 메서드들 ======
    // user.id 기준 조회
    List<UserStore> findByUser_Id(Long userId);

    // user.id + position
    List<UserStore> findByUser_IdAndPosition(Long userId, Position position);

    // user.id + store.id (관계 존재 여부/조회)
    Optional<UserStore> findByUser_IdAndStore_Id(Long userId, Long storeId);
    boolean existsByUser_IdAndStore_Id(Long userId, Long storeId);

    // user.id 기준 최초 등록(활성 매장 기본값)
    Optional<UserStore> findFirstByUser_IdOrderByCreatedAtAsc(Long userId);

    // store.id 기준 전체(사장+알바)
    List<UserStore> findByStore_Id(Long storeId);

    // store.id + position
    List<UserStore> findByStore_IdAndPosition(Long storeId, Position position);


    // ====== 레거시(기존 코드 호환) 브리지 메서드들 ======
    // 기존: findByUserId(Long)
    default List<UserStore> findByUserId(Long userId) {
        return findByUser_Id(userId);
    }

    // 기존: findByUserIdAndPosition(Long, Position)
    default List<UserStore> findByUserIdAndPosition(Long userId, Position position) {
        return findByUser_IdAndPosition(userId, position);
    }

    // 기존: findByUserIdAndStoreId(Long, Long)
    default Optional<UserStore> findByUserIdAndStoreId(Long userId, Long storeId) {
        return findByUser_IdAndStore_Id(userId, storeId);
    }

    // 기존: existsByUserIdAndStoreId(Long, Long)
    default boolean existsByUserIdAndStoreId(Long userId, Long storeId) {
        return existsByUser_IdAndStore_Id(userId, storeId);
    }

    // 기존: findFirstByUserIdOrderByCreatedAtAsc(Long)
    default Optional<UserStore> findFirstByUserIdOrderByCreatedAtAsc(Long userId) {
        return findFirstByUser_IdOrderByCreatedAtAsc(userId);
    }

    // 기존: findByStoreId(Long)
    default List<UserStore> findByStoreId(Long storeId) {
        return findByStore_Id(storeId);
    }

    // 기존: findByStoreIdAndPosition(Long, Position)
    default List<UserStore> findByStoreIdAndPosition(Long storeId, Position position) {
        return findByStore_IdAndPosition(storeId, position);
    }

    // 기존에 String 포지션으로 부르던 곳 호환용
    default List<UserStore> findByStore_IdAndPosition(Long storeId, String position) {
        if (position == null || position.isBlank()) return List.of();
        try {
            return findByStore_IdAndPosition(storeId, Position.valueOf(position.toUpperCase()));
        } catch (IllegalArgumentException ex) {
            // 잘못된 문자열이면 빈 리스트 반환(또는 로그 후 빈 리스트)
            return List.of();
        }
    }
}
