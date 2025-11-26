package com.example.inventory.service.impl;

import com.example.inventory.model.Task;
import com.example.inventory.model.entityEnum.Status;
import com.example.inventory.repository.TaskRepository;
import com.example.inventory.service.EmailService;
import com.example.inventory.service.ReminderService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReminderServiceImpl implements ReminderService {
    private final TaskRepository taskRepository;
    private final EmailService emailService;

    public ReminderServiceImpl(TaskRepository taskRepository, EmailService emailService) {
        this.taskRepository = taskRepository;
        this.emailService = emailService;
    }

    @Scheduled(cron = "0 * * * * *")
    @Override
    public void scanForDeadlines(){
        System.out.println("----Quét deadline---");
        LocalDateTime now = LocalDateTime.now();

        List<Task> dueTasks = taskRepository.findByStatusNotAndDeadlineBeforeAndIsRemindFalse(
                Status.DONE,
                now
        );

        for (Task task : dueTasks) {
            sendNotification(task);

            task.setRemind(true);
            taskRepository.save(task);
        }
    }

    private void sendNotification(Task task) {
        // Cách 1: Gửi Email (Backend làm được ngay)
        String emailUser = task.getUser().getEmail();
        String subject = "⏰ THÔNG BÁO: Đã đến hạn deadline!";
        String body = "Task '" + task.getTitle() + "' đã đến hạn vào lúc " + task.getDeadline() + ". Hãy hoàn thành ngay!";

        try {
            emailService.sendEmail(emailUser, subject, body);
            System.out.println(">>> Đã gửi mail nhắc nhở cho task: " + task.getTitle());
        } catch (Exception e) {
            System.out.println(">>> Lỗi gửi mail: " + e.getMessage());
        }

        // Cách 2 (Giả lập nhạc): Backend chỉ có thể in ra log console
        // Để có nhạc thật, Frontend (React/Web) phải gọi API kiểm tra hoặc dùng WebSocket
        System.out.println("♫ ♫ ♫ BEEP BEEP! Task ID " + task.getId() + " Overdue! ♫ ♫ ♫");
    }


    @Override
    public List<Task> getOverdueAlerts(String username) {
        // Lấy thời gian hiện tại
        LocalDateTime now = LocalDateTime.now();

        // Gọi Repository để tìm các task:
        // - Của User này
        // - Deadline nhỏ hơn hiện tại (Đã quá hạn)
        // - Trạng thái KHÁC DONE (Chưa làm xong)
        // - Chưa tắt thông báo (isAlertDismissed = false)
        return taskRepository.findByUser_UsernameAndDeadlineBeforeAndStatusNotAndIsAlertDismissedFalse(
                username,
                now,
                Status.DONE
        );
    }

    @Override
    public void dismissTaskAlert(Long taskId, String username) {
        // 1. Tìm Task
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Task ID: " + taskId));

        // 2. Kiểm tra quyền sở hữu (Bảo mật)
        // Tránh trường hợp user A tắt thông báo của user B
        if (!task.getUser().getUsername().equals(username)) {
            throw new RuntimeException("Bạn không có quyền tắt thông báo của task này!");
        }

        // 3. Đánh dấu đã tắt thông báo (Flag = true)
        task.setAlertDismissed(true);

        // 4. Lưu lại vào DB
        taskRepository.save(task);
    }
}
