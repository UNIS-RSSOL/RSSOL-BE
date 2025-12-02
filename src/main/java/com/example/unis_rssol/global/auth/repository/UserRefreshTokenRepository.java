package com.example.unis_rssol.global.auth.repository;

import com.example.unis_rssol.global.auth.entity.UserRefreshToken;
import com.example.unis_rssol.domain.user.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRefreshTokenRepository extends JpaRepository<UserRefreshToken, Long> {
    Optional<UserRefreshToken> findByRefreshToken(String refreshToken);
    void deleteByUser(AppUser user);
}
