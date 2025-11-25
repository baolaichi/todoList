package com.example.inventory.service;

public interface BlacklistService {
    void blacklistToken(String token);
    public boolean isBlacklisted(String token);
}
