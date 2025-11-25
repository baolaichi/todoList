package com.example.inventory.service;

import com.example.inventory.model.Users;

public interface AuthService {
    public String register(Users request);
    public String login(String username, String password);
    public void forgotPassword(String email);
    public void resetPassword(String email, String otp, String newPassword);
}
