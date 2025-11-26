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

    @Override
    @Scheduled(cron = "0 * * * * *") // Chạy mỗi phút
    // @Transactional -> KHÔNG NÊN để @Transactional ở cấp độ hàm lớn này
    // Vì nếu 1 task lỗi, nó có thể rollback toàn bộ các task khác.
    public void scanForDeadlines() {
        System.out.println("---- Bắt đầu quét Deadline ----");
        LocalDateTime now = LocalDateTime.now();

        // 1. Lấy danh sách tất cả task đến hạn của TẤT CẢ mọi người
        List<Task> dueTasks = taskRepository.findByStatusNotAndDeadlineBeforeAndIsRemindFalse(
                Status.DONE,
                now
        );

        if (dueTasks.isEmpty()) return;

        System.out.println("Tìm thấy " + dueTasks.size() + " task cần nhắc nhở.");

        // 2. Duyệt từng task để xử lý
        for (Task task : dueTasks) {
            try {
                // --- LOGIC XỬ LÝ RIÊNG BIỆT CHO TỪNG TASK ---

                // a. Gửi thông báo (Email sẽ lấy từ task -> user -> email)
                // Nên task của ai thì mail gửi về đúng người đó.
                sendNotification(task);

                // b. Đánh dấu đã nhắc
                task.setIsRemind(true); // Hoặc setRemind(true) tuỳ getter/setter của bạn

                // c. Lưu ngay lập tức (Save từng cái)
                taskRepository.save(task);

            } catch (Exception e) {
                // Nếu task này bị lỗi (ví dụ mail sai, db lỗi), in log và BỎ QUA
                // Để vòng lặp tiếp tục chạy sang task tiếp theo
                System.err.println("Lỗi xử lý task ID " + task.getId() + ": " + e.getMessage());
            }
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
        task.setIsAlertDismissed(true);

        // 4. Lưu lại vào DB
        taskRepository.save(task);
    }
}
