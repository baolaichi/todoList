package com.example.inventory.model;

import com.example.inventory.model.entityEnum.Priority;
import com.example.inventory.model.entityEnum.Status;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Data
@Table(name = "task_groups") // Tên bảng riêng biệt
public class TaskGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;
    private LocalDateTime deadline;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    private Status status = Status.TODO;

    @Enumerated(EnumType.STRING)
    private Priority priority;

    // Liên kết với Nhóm (Bắt buộc)
    @ManyToOne
    @JoinColumn(name = "group_id", nullable = false)
    @ToString.Exclude           // <--- THÊM
    @EqualsAndHashCode.Exclude
    private GroupTodo group;

    // Người tạo task (Leader hoặc member tạo)
    @ManyToOne
    @JoinColumn(name = "created_by")
    @ToString.Exclude           // <--- QUAN TRỌNG NHẤT
    @EqualsAndHashCode.Exclude
    private Users createdBy;

    // Danh sách người được giao việc (Quan hệ nhiều-nhiều)
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "task_group_assignees", // Bảng trung gian riêng
            joinColumns = @JoinColumn(name = "task_group_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<Users> assignees = new HashSet<>();

    @Column(name = "is_reminded")
    private Boolean isRemind = false;

    @Column(name = "is_alert_dismissed")
    private Boolean isAlertDismissed = false;
}