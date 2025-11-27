package com.example.inventory.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Autowired
    private JwtFilter jwtFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. KÍCH HOẠT CORS Ở ĐÂY (Quan trọng nhất)
                // Nó sẽ sử dụng bean corsConfigurationSource bên dưới để xử lý các request OPTIONS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // Cho phép các API Auth truy cập công khai
                        .requestMatchers("/api/auth/**").permitAll()
                        // Cho phép thông websocket
                        .requestMatchers("/ws/**").permitAll()

                        // Cấu hình quyền truy cập
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        .requestMatchers("/api/user/**").hasAnyRole("OWNER", "ADMIN", "VIEWER")

                        // Tất cả request còn lại phải đăng nhập
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                );

        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    // 2. CẤU HÌNH CHI TIẾT CHO CORS
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Cho phép Frontend chạy ở cổng 5173 (React/Vite) gọi vào
        configuration.setAllowedOrigins(List.of("http://localhost:5173"));

        // Cho phép các phương thức HTTP này
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

        // Cho phép mọi loại Header (quan trọng để gửi Authorization: Bearer ...)
        configuration.setAllowedHeaders(List.of("*"));

        // Cho phép gửi kèm credentials (nếu sau này cần cookie)
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Áp dụng cấu hình này cho mọi đường dẫn trong API
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}