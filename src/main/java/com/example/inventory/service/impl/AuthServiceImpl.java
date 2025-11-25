package com.example.inventory.service.impl;

import com.example.inventory.config.JwtUtil;
import com.example.inventory.model.TokenBlacklist;
import com.example.inventory.model.Users;
import com.example.inventory.repository.BlacklistRepository;
import com.example.inventory.repository.UserRepository;
import com.example.inventory.service.AuthService;
import com.example.inventory.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@Slf4j
public class AuthServiceImpl implements AuthService{
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final BlacklistRepository blacklistRepository;
    private final EmailService emailService;

    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder,
                           AuthenticationManager authenticationManager, JwtUtil jwtUtil,
                           BlacklistRepository blacklistRepository, EmailService emailService)
    {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.blacklistRepository = blacklistRepository;
        this.emailService = emailService;
    }

    @Override
    public String register(Users request){
        if(userRepository.existsByUsername(request.getUsername())){
            throw new RuntimeException("Username already exists");
        }

        request.setPassword(passwordEncoder.encode(request.getPassword()));
        userRepository.save(request);

        return "Register success";
    }

    @Override
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

    @Override
    public void forgotPassword(String email){
        Users users = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("Không tìm thấy email"));
        String otp = String.valueOf(new Random().nextInt(900000) +100000);

        users.setOtp(otp);
        users.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
        userRepository.save(users);

        try {
            // Cố gắng gửi mail (nếu mạng cho phép)
            emailService.sendEmail(email, "OTP", "Mã là: " + otp);
        } catch (Exception e) {
            // Nếu mạng chặn, catch lỗi và CHỈ IN RA CONSOLE
            System.err.println("KHÔNG GỬI ĐƯỢC MAIL DO MẠNG NỘI BỘ!");
            System.out.println(">>> MÃ OTP CỦA BẠN LÀ: " + otp + " <<<");
        }

    }

    @Override
    public void resetPassword(String email, String otp, String newPassword){
        Users users = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy email"));
        if (users.getOtp() == null || !users.getOtp().equals(otp)){
            throw  new RuntimeException("Mã OTP không đúng");
        }else if (users.getOtpExpiry().isBefore(LocalDateTime.now())){
            throw new RuntimeException("Mã hết hạn! vui lòng lấy lại mã");
        }else {
            users.setPassword(passwordEncoder.encode(newPassword));
        }

        users.setOtp(null);
        users.setOtpExpiry(null);
        userRepository.save(users);
    }

}
