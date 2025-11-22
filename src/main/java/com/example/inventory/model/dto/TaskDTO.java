package com.example.inventory.model.dto;

import com.example.inventory.model.Users;
import com.example.inventory.model.entityEnum.Priority;
import com.example.inventory.model.entityEnum.Status;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class TaskDTO {
    private String title;
    private String description;
    private LocalDateTime deadline;
    private LocalDateTime creatAt = LocalDateTime.now();
    private LocalDateTime updatedAt;
    private Long userId;
    private Status status;
    private Priority priority;
}
