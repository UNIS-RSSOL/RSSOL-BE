package com.example.unis_rssol.domain.store.setting;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StoreSettingRepository extends JpaRepository<StoreSetting, Long> {
    Optional<StoreSetting> findByStoreId(Long storeId);
    boolean existsByStoreId(Long storeId);
}

