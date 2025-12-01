package com.example.inventory.service.impl;

import com.example.inventory.model.Users;
import com.example.inventory.model.dto.AdminDashboardDTO;
import com.example.inventory.repository.GroupRepository;
import com.example.inventory.repository.TaskRepository;
import com.example.inventory.repository.UserRepository;
import com.example.inventory.service.AdminDashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminDashboardServiceImpl implements AdminDashboardService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private GroupRepository groupRepository;
    @Autowired
    private TaskRepository taskRepository;

    @Override
    public AdminDashboardDTO getDashboardStats(){
        long totalUsers = userRepository.count();
        long totalGroups = groupRepository.count();
        long totalTasks = taskRepository.count();

        return new AdminDashboardDTO(totalUsers, totalGroups, totalTasks, 0, 0);
    }

    public List<Users> getAllUsers() {
        return userRepository.findAll();
    }

}
