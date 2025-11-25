package com.example.inventory.service.impl;

import com.example.inventory.model.TokenBlacklist;
import com.example.inventory.repository.BlacklistRepository;
import com.example.inventory.service.BlacklistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class BlacklistServiceImpl implements BlacklistService {
    @Autowired
    private BlacklistRepository blacklistRepository;

    @Override
    public void blacklistToken(String token){
        TokenBlacklist blacklist = new TokenBlacklist();
        blacklist.setToken(token);
        blacklist.setExpiryDate(LocalDateTime.now().plusHours(24));
        blacklistRepository.save(blacklist);
    }

    @Override
    public boolean isBlacklisted(String token) {
        return blacklistRepository.existsByToken(token);
    }
}
