package com.example.inventory.repository;

import com.example.inventory.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // Tìm danh sách thông báo chưa đọc của một user cụ thể
    // (Dùng để hiển thị số đỏ trên cái chuông ở Frontend)
    List<Notification> findByUser_UsernameAndIsReadFalse(String username);

    // Tìm tất cả thông báo của user (Sắp xếp mới nhất lên đầu)
    List<Notification> findByUser_UsernameOrderByCreatedAtDesc(String username);
}