package com.example.unis_rssol.domain.user.repository;

import com.example.unis_rssol.domain.user.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByProviderAndProviderId(String provider, String providerId);

    //로컬개발용
    Optional<AppUser> findByEmail(String email);
}