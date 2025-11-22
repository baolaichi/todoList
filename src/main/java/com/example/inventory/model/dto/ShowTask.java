package com.example.inventory.model.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ShowTask {
    private String title;
    private LocalDateTime deadline;
    private LocalDateTime createAt;

    public ShowTask(String title, LocalDateTime deadline, LocalDateTime createAt) {
        this.title = title;
        this.deadline = deadline;
        this.createAt = createAt;
    }
}
