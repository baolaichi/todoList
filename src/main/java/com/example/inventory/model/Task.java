package com.example.inventory.model;

import com.example.inventory.model.entityEnum.Priority;
import com.example.inventory.model.entityEnum.Status;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "task")
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String title;
    private String description;
    private LocalDateTime deadline;
    private LocalDateTime creatAt;
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    private Status status = Status.TODO;

    @Enumerated(EnumType.STRING)
    private Priority priority;

    // --- CÁC TRƯỜNG CÁ NHÂN ---
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private Users user; // Chủ sở hữu task cá nhân

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category; // Phân loại cá nhân (nếu có)

    // --- CÁC TRƯỜNG CẤU HÌNH (Báo thức/Thông báo) ---
    @Column(name = "is_reminded")
    private Boolean isRemind = false;

    @Column(name = "is_alert_dismissed")
    private Boolean isAlertDismissed = false;

    @Column(name = "is_email_sent")
    private Boolean emailSent = false;

}