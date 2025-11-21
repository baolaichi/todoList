package com.example.inventory.service.impl;

import com.example.inventory.config.CustomUserDetails;
import com.example.inventory.model.Users;
import com.example.inventory.repository.UserRepository;
import com.example.inventory.service.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsServiceImpl implements CustomUserDetailsService, UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException{
        Users user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User mot found"));
        return new CustomUserDetails(user);
    }

    @Override
    public Users getUserEntityByUsername(String username){
        return userRepository.findByUsername(username).orElse(null);
    }

}
