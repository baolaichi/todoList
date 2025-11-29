package com.example.inventory.model.dto;

import com.example.inventory.model.entityEnum.Priority;
import com.example.inventory.model.entityEnum.Status;
import lombok.Data;

import java.util.List;

@Data
public class TaskGroupCreateRequest {
    private String title;
    private String description;

    // Dùng String để chấp nhận mọi định dạng (T, dấu cách...)
    private String deadline;

    private Priority priority;
    private Status status;

    private Long groupId;
    private List<Long> assigneeIds;
}