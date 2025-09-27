package com.example.unis_rssol.auth.repository;

import com.example.unis_rssol.auth.entity.UserRefreshToken;
import com.example.unis_rssol.user.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRefreshTokenRepository extends JpaRepository<UserRefreshToken, Long> {
    Optional<UserRefreshToken> findByRefreshToken(String refreshToken);
    void deleteByUser(AppUser user);
}
