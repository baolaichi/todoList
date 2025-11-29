package com.example.inventory.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WorkLogDTO {
    private Long id;
    private String content;
    private LocalDateTime createdAt;
    private String reporterName;

    private String taskTitle;
    private Long taskId;
}
