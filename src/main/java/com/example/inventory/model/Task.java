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

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private Users user; // <-- Sửa thành 'user

    @Enumerated(EnumType.STRING)
    private Status status = Status.TODO;
    @Enumerated(EnumType.STRING)
    private Priority priority;


    @ManyToOne
    @JoinColumn(name = "group_id")
    private GroupTodo group;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "is_reminded")
    private Boolean isRemind = false;

    @Column(name = "is_alert_dismissed")
    private Boolean isAlertDismissed = false;
}
