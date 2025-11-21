package com.example.inventory.service;

import com.example.inventory.model.Users;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public interface CustomUserDetailsService {
    public UserDetails loadUserByUsername(String username);
    public Users getUserEntityByUsername(String username);
}
