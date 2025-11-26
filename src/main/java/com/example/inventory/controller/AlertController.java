package com.example.inventory.controller;

import com.example.inventory.service.ReminderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user/alerts")
public class AlertController {

    @Autowired
    private ReminderService reminderService;

    // API để Web gọi liên tục (Polling) xem có nhạc không
    @GetMapping
    public ResponseEntity<?> getAlerts() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(reminderService.getOverdueAlerts(username));
    }

    // API để User tắt nhạc/tắt popup
    @PatchMapping("/{taskId}/dismiss")
    public ResponseEntity<?> dismiss(@PathVariable Long taskId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        reminderService.dismissTaskAlert(taskId, username);
        return ResponseEntity.ok("Đã tắt thông báo");
    }
}
