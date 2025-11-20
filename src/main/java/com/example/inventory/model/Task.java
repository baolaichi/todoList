package com.example.inventory.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
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
    private LocalDate deadline;
    private LocalDateTime creatAt;
    private LocalDateTime updatedAt;

    @ManyToOne
    private Users user_id;

    private enum status{
        TODO,
        IN_PROGRESS,
        DONE
    }

    private enum priority{
        LOW,
        MEDIUM,
        HIGH
    }
}
