package com.example.inventory.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "system_reviews")
@Data
public class SystemReview {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String authorName;
    private int rating; // 1-5

    @Column(columnDefinition = "TEXT")
    private String comment;

    private boolean isApproved = false;

    private LocalDateTime createdAt = LocalDateTime.now();
}
