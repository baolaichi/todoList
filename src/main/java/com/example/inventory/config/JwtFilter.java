package com.example.inventory.config;

import com.example.inventory.service.CustomUserDetailsService;
import io.jsonwebtoken.io.SerialException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtFilter extends OncePerRequestFilter {
    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws SerialException, IOException, ServletException {

        // 1. Lấy header
        String header = request.getHeader("Authorization");
        String token = null;
        String username = null;

        try { // <--- THÊM TRY CATCH ĐỂ BẮT LỖI TOKEN
            if (header != null && header.startsWith("Bearer ")) {
                token = header.substring(7);
                username = jwtUtil.extractUsername(token);
            }

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                // --- LOG ĐỂ DEBUG (Xem console khi chạy) ---
                System.out.println("DEBUG: Đang check User: " + username);
                System.out.println("DEBUG: Quyền trong DB: " + userDetails.getAuthorities());
                // -------------------------------------------

                if (jwtUtil.validateToken(token, userDetails)) {
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                } else {
                    System.out.println("DEBUG: Token không hợp lệ hoặc hết hạn!");
                }
            }
        } catch (Exception e) {
            // Nếu token hết hạn hoặc lỗi, in ra để biết
            System.out.println("DEBUG ERROR: Lỗi xác thực token: " + e.getMessage());
            // e.printStackTrace(); // Bật dòng này nếu muốn xem full lỗi
        }

        filterChain.doFilter(request, response);
    }



}
