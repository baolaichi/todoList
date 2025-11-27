package com.example.inventory.model.dto;

import com.example.inventory.model.entityEnum.Priority;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class TaskGroupDTO {
    private String title;
    private String description;
    private LocalDateTime deadline;
    private Priority priority;

    private Long groupId;       // ID nhóm
    private Long assignToUserId; // ID người được giao việc
}
