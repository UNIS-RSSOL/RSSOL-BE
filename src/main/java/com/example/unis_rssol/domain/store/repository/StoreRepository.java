package com.example.unis_rssol.domain.store.repository;

import com.example.unis_rssol.domain.store.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StoreRepository extends JpaRepository<Store, Long> {
    Optional<Store> findByStoreCode(String storeCode);

    boolean existsByBusinessRegistrationNumber(String businessRegistrationNumber);
}
