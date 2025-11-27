package com.example.inventory.controller;

import com.example.inventory.model.Users;
import com.example.inventory.model.dto.ResetPasswordDTO;
import com.example.inventory.model.dto.UserProfileDTO;
import com.example.inventory.service.AuthService;
import com.example.inventory.service.BlacklistService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private BlacklistService blacklistService;

    public AuthController(AuthService authService, BlacklistService blacklistService) {
        this.authService = authService;
        this.blacklistService = blacklistService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody Users request){
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request){

        try {
            String token = authService.login(request.username(), request.password());
            return ResponseEntity.ok(new AuthResponse(token));
        }catch (RuntimeException ex){
            // Xử lý lỗi xác thực và trả về 401
            return ResponseEntity.
                    status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request){
        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);

            blacklistService.blacklistToken(token);

            return ResponseEntity.ok("Đăng xuất thành công! Token đã bị vô hiệu hóa.");
        }

        return ResponseEntity.badRequest().body("Không tìm thấy Token để đăng xuất");

    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestParam String email) {
        try {
            authService.forgotPassword(email);
            return ResponseEntity.ok("Mã OTP đã được gửi đến email của bạn!");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordDTO request) {
        try {
            authService.resetPassword(request.getEmail(), request.getOtp(), request.getNewPassword());
            return ResponseEntity.ok("Đặt lại mật khẩu thành công! Hãy đăng nhập lại.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }


    public static record AuthRequest(String username, String password) {}
    public static record AuthResponse(String token) {}

}
