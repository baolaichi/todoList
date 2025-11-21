package com.example.inventory.controller;

import com.example.inventory.model.Users;
import com.example.inventory.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody Users request){
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request){

//        try {
//            String token = authService.login(request.username(), request.password());
//            return ResponseEntity.ok(new AuthResponse(token));
//        }catch (RuntimeException ex){
//            return ResponseEntity.
//                    status(HttpStatus.UNAUTHORIZED)
//                    .body(Map.of("error", ex.getMessage()));
//        }
        String token = authService.login(request.username(), request.password());
        return ResponseEntity.ok(new AuthResponse(token));

    }

    public static record AuthRequest(String username, String password) {}
    public static record AuthResponse(String token) {}

}
