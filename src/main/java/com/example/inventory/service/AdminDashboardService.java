package com.example.inventory.service;

import com.example.inventory.model.Users;
import com.example.inventory.model.dto.AdminDashboardDTO;

import java.util.List;

public interface AdminDashboardService {
    public AdminDashboardDTO getDashboardStats();
    public List<Users> getAllUsers();
}
