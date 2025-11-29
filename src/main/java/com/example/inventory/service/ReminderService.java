package com.example.inventory.service;

import com.example.inventory.model.Task;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

public interface ReminderService {

    void scanForDeadlines();

    List<Object> getOverdueAlerts(String username);

    // 2. Tắt báo động cho một task cụ thể
    void dismissTaskAlert(Long taskId, String username);
}
