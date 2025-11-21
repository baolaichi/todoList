package com.example.inventory.service.impl;

import com.example.inventory.config.JwtUtil;
import com.example.inventory.model.Users;
import com.example.inventory.repository.UserRepository;
import com.example.inventory.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AuthServiceImpl implements AuthService{
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
    }

    public String register(Users request){
        if(userRepository.existsByUsername(request.getUsername())){
            throw new RuntimeException("Username already exists");
        }

        request.setPassword(passwordEncoder.encode(request.getPassword()));
        userRepository.save(request);

        return "Register success";
    }

    public String login(String username, String password){
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
            log.info("Start create token after enter information");
            return jwtUtil.generateToken(username);
        }catch (Exception e){
            log.error("lỗi nhâp sai tài khoản: " + username + "||" + password);
            return "Error: " + e.getMessage();
        }
    }

}
