package com.rssolplan.api.global.fordevToken;

import com.rssolplan.api.domain.user.User;
import com.rssolplan.api.domain.user.UserRepository;
import com.rssolplan.api.global.config.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class DevTokenController {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwt;

    // 개발용: 이메일만 보내면 Access Token 바로 발급
    @PostMapping("/dev-token")
    public ResponseEntity<String> generateDevToken(@RequestBody DevTokenRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("해당 이메일의 유저가 없습니다."));

        String accessToken = jwt.generateAccess(user.getId());

        return ResponseEntity.ok(accessToken);
    }
}
