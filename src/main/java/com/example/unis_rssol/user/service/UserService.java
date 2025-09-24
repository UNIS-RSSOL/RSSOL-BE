package com.example.unis_rssol.user.service;

import com.example.unis_rssol.user.dto.*;
import com.example.unis_rssol.user.entity.AppUser;
import com.example.unis_rssol.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service @RequiredArgsConstructor
public class UserService {
    private final AppUserRepository users;
    public SelectRoleResponse setRole(Long userId, SelectRoleRequest req){
        AppUser u = users.findById(userId).orElseThrow();
        // 역할은 user_store에서 실제 반영되지만, 선택값을 응답해 프론트 분기용으로 반환
        return new SelectRoleResponse(u.getId(), req.getRole(), false);
    }
}