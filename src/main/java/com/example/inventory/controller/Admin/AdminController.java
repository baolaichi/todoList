package com.example.inventory.controller.Admin;

import com.example.inventory.model.Users;
import com.example.inventory.model.dto.AdminDashboardDTO;
import com.example.inventory.service.AdminDashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    @Autowired
    private AdminDashboardService adminDashboardService;

    @GetMapping("/stats")
    public ResponseEntity<AdminDashboardDTO> getStats(){
        return ResponseEntity.ok(adminDashboardService.getDashboardStats());
    }

    @GetMapping("/users")
    public ResponseEntity<List<Users>> getAllUsers() {
        return ResponseEntity.ok(adminDashboardService.getAllUsers());
    }
}
