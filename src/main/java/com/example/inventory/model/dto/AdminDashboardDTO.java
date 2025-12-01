package com.example.inventory.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminDashboardDTO {
    private long totalUsers;
    private long totalGroups;
    private long totalTasks;
    private long totalFiles;
    private long totalUsersInMoth;
}
