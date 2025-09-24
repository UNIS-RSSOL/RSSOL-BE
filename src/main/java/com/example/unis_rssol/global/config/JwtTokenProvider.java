package com.example.unis_rssol.global.config;

import io.jsonwebtoken.*;
import org.springframework.stereotype.Component;
import java.util.Date;

@Component
public class JwtTokenProvider {
    private final String SECRET = "change-this-to-env";
    private final long ACCESS_MS = 1000L * 60 * 60;         // 1h
    private final long REFRESH_MS = 1000L * 60 * 60 * 24 * 14; // 14d

    public String generateAccess(Long userId) {
        return Jwts.builder()
                .setSubject(userId.toString())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + ACCESS_MS))
                .signWith(SignatureAlgorithm.HS256, SECRET)
                .compact();
    }
    public String generateRefresh(Long userId) {
        return Jwts.builder()
                .setSubject(userId.toString())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + REFRESH_MS))
                .signWith(SignatureAlgorithm.HS256, SECRET)
                .compact();
    }
    public boolean validate(String token) {
        try { Jwts.parser().setSigningKey(SECRET).parseClaimsJws(token); return true; }
        catch (Exception e) { return false; }
    }
    public Long getUserId(String token) {
        Claims c = Jwts.parser().setSigningKey(SECRET).parseClaimsJws(token).getBody();
        return Long.parseLong(c.getSubject());
    }
    public Date getRefreshExpiry() { return new Date(System.currentTimeMillis() + REFRESH_MS); }
}
