package com.example.inventory.model.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ShowTask {
    private String title;
    private LocalDateTime deadline;
    private LocalDateTime createtAt;

    public ShowTask(String title, LocalDateTime deadline, LocalDateTime createtAt) {
        this.title = title;
        this.deadline = deadline;
        this.createtAt = createtAt;
    }
}
