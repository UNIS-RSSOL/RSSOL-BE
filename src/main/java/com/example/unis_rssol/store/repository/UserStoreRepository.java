package com.example.unis_rssol.store.repository;

import com.example.unis_rssol.store.entity.UserStore;
import com.example.unis_rssol.store.entity.UserStore.Position;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserStoreRepository extends JpaRepository<UserStore, Long> {
    // 내 모든 매장 (사장/알바 공통)
    List<UserStore> findByUserId(Long userId);

    // 내 매장 중 역할로 필터
    List<UserStore> findByUserIdAndPosition(Long userId, Position position);

    // 특정 매장과의 매핑 존재 여부/조회
    Optional<UserStore> findByUserIdAndStoreId(Long userId, Long storeId);

    boolean existsByUserIdAndStoreId(Long userId, Long storeId);

    // 최초 등록 매장(활성 매장 미설정시 기본값용)
    Optional<UserStore> findFirstByUserIdOrderByCreatedAtAsc(Long userId);
}
