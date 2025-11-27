package com.example.inventory.controller;

import com.example.inventory.model.dto.UserProfileDTO;
import com.example.inventory.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserController {
    @Autowired
    private AuthService authService;

    @GetMapping("/profile")
    public ResponseEntity<UserProfileDTO> getProfile() {
        // 1. Lấy username từ Token (An toàn vì API này đã được bảo vệ bởi SecurityConfig)
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        // 2. Gọi Service lấy dữ liệu
        return ResponseEntity.ok(authService.getMyProfile(username));
    }
}
