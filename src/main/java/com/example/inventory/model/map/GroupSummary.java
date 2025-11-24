package com.example.inventory.model.map;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;

@Entity
@Data
@Immutable // <--- Quan trọng: Báo cho Hibernate biết không được update vào View này
@Table(name = "view_group_summary") // Tên View trong DB
public class GroupSummary {
    @Id
    private Long id; // View vẫn cần ID

    private String name;
    private String description;

    @Column(name = "member_count")
    private int memberCount; // Cột này lấy từ View

    private LocalDateTime createdAt;

}