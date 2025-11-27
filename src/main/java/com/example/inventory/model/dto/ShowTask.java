package com.example.inventory.model.dto;

import com.example.inventory.model.entityEnum.Priority;
import com.example.inventory.model.entityEnum.Status;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ShowTask {
    private Long id;
    private String title;
    private LocalDateTime deadline;
    private LocalDateTime createtAt;
    private Priority priority;
    private Status status;

}
