package com.example.inventory.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String message; // Nội dung thông báo (VD: "Cảnh báo dung lượng...")

    @Column(name = "is_read")
    private boolean isRead = false; // Trạng thái đã đọc/chưa đọc

    private LocalDateTime createdAt = LocalDateTime.now();

    // Thông báo này gửi cho ai?
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;
}