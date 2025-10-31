package com.example.unis_rssol.fordev;

import com.example.unis_rssol.global.config.JwtTokenProvider;
import com.example.unis_rssol.user.entity.AppUser;
import com.example.unis_rssol.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class DevTokenController {

    private final AppUserRepository userRepository;
    private final JwtTokenProvider jwt;

    // 개발용: 이메일만 보내면 Access Token 바로 발급
    @PostMapping("/dev-token")
    public ResponseEntity<String> generateDevToken(@RequestBody DevTokenRequest request) {
        AppUser user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("해당 이메일의 유저가 없습니다."));

        String accessToken = jwt.generateAccess(user.getId());

        return ResponseEntity.ok(accessToken);
    }
}
