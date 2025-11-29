package com.example.inventory.service.impl;

import com.example.inventory.model.Task;
import com.example.inventory.model.TaskGroup;
import com.example.inventory.model.Users;
import com.example.inventory.model.entityEnum.Status;
import com.example.inventory.repository.TaskGroupRepository;
import com.example.inventory.repository.TaskRepository;
import com.example.inventory.service.EmailService;
import com.example.inventory.service.ReminderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReminderServiceImpl implements ReminderService {
    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private EmailService emailService;
    @Autowired
    private TaskGroupRepository taskGroupRepository;

    // --- 1. CHẠY NGẦM (GỬI EMAIL) ---
    @Override
    @Scheduled(cron = "0 * * * * *") // Chạy mỗi phút
    @Transactional
    public void scanForDeadlines() {
        LocalDateTime now = LocalDateTime.now();

        // ----------------------------------------
        // A. XỬ LÝ TASK CÁ NHÂN
        // ----------------------------------------
        List<Task> dueTasks = taskRepository.findByStatusNotAndDeadlineBeforeAndIsRemindFalse(
                Status.DONE,
                now
        );

        List<Task> safeTaskList = new ArrayList<>(dueTasks);
        for (Task task : safeTaskList) {
            try {
                // Task cá nhân dùng .getUser()
                sendNotification(task.getUser(), task.getTitle(), task.getDeadline().toString());

                task.setIsRemind(true);
                taskRepository.save(task);
            } catch (Exception e) {
                System.err.println("Lỗi task cá nhân ID " + task.getId() + ": " + e.getMessage());
            }
        }

        // ----------------------------------------
        // B. XỬ LÝ TASK NHÓM (TaskGroup)
        // ----------------------------------------
        List<TaskGroup> dueGroupTasks = taskGroupRepository.findByStatusNotAndDeadlineBeforeAndIsRemindFalse(
                Status.DONE,
                now
        );

        List<TaskGroup> safeGroupTaskList = new ArrayList<>(dueGroupTasks);
        for (TaskGroup groupTask : safeGroupTaskList) {
            try {
                // Gửi cho tất cả người được giao
                for (Users assignee : groupTask.getAssignees()) {
                    sendNotification(assignee, "[Nhóm] " + groupTask.getTitle(), groupTask.getDeadline().toString());
                }

                // Gửi cho người tạo (Nếu cần) - SỬA LỖI Ở ĐÂY: dùng getCreatedBy() thay vì getUser()
                if (groupTask.getCreatedBy() != null) {
                    sendNotification(groupTask.getCreatedBy(), "[Nhóm-Admin] " + groupTask.getTitle(), groupTask.getDeadline().toString());
                }

                groupTask.setIsRemind(true);
                taskGroupRepository.save(groupTask);
            } catch (Exception e) {
                System.err.println("Lỗi task nhóm ID " + groupTask.getId() + ": " + e.getMessage());
            }
        }
    }

    // Hàm gửi mail chung
    private void sendNotification(Users user, String title, String deadline) {
        if (user == null || user.getEmail() == null) return;

        String subject = "⏰ THÔNG BÁO: Đã đến hạn deadline!";
        String body = "Công việc '" + title + "' đã đến hạn vào lúc " + deadline + ". Hãy kiểm tra và hoàn thành ngay!";

        try {
            emailService.sendEmail(user.getEmail(), subject, body);
            System.out.println(">>> Đã gửi mail: " + title + " -> " + user.getEmail());
        } catch (Exception e) {
            System.err.println(">>> Lỗi gửi mail: " + e.getMessage());
        }
    }

    // --- 2. API CHO FRONTEND (HIỆN POPUP/NHẠC) ---

    @Override
    public List<Object> getOverdueAlerts(String username) {
        LocalDateTime now = LocalDateTime.now();
        List<Object> alerts = new ArrayList<>();

        // Task cá nhân
        List<Task> personalTasks = taskRepository.findByUser_UsernameAndStatusNotAndDeadlineBeforeAndIsAlertDismissedFalse(
                username, Status.DONE, now
        );
        alerts.addAll(personalTasks);

        // Task nhóm
        List<TaskGroup> groupTasks = taskGroupRepository.findByAssignees_UsernameAndStatusNotAndDeadlineBeforeAndIsAlertDismissedFalse(
                username, Status.DONE, now
        );
        alerts.addAll(groupTasks);

        return alerts;
    }


    @Override
    public void dismissTaskAlert(Long taskId, String username) {
        // Tắt thông báo Task Cá nhân
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Task Cá nhân ID: " + taskId));

        if (!task.getUser().getUsername().equals(username)) {
            throw new RuntimeException("Bạn không có quyền!");
        }

        task.setIsAlertDismissed(true);
        taskRepository.save(task);
    }

    public void dismissGroupTaskAlert(Long taskGroupId, String username) {
        TaskGroup taskGroup = taskGroupRepository.findById(taskGroupId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Task Group ID: " + taskGroupId));

        // Check quyền: Người tạo (createdBy) HOẶC Người được giao (assignees)
        boolean isOwner = taskGroup.getCreatedBy().getUsername().equals(username); // <-- SỬA getUser() THÀNH getCreatedBy()
        boolean isAssignee = taskGroup.getAssignees().stream().anyMatch(u -> u.getUsername().equals(username));

        if (!isOwner && !isAssignee) {
            throw new RuntimeException("Bạn không có quyền tắt thông báo này!");
        }

        taskGroup.setIsAlertDismissed(true);
        taskGroupRepository.save(taskGroup);
    }


}