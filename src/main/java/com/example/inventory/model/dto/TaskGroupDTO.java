package com.example.inventory.model.dto;

import lombok.Data;

@Data
public class TaskGroupDTO extends TaskDTO{
    private Long groupId;
    private Long assignToUserId;
}
