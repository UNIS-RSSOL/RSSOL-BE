package com.rssolplan.api.domain.store;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StoreRepository extends JpaRepository<Store, Long> {
    Optional<Store> findByStoreCode(String storeCode);

    boolean existsByBusinessRegistrationNumber(String businessRegistrationNumber);
}
