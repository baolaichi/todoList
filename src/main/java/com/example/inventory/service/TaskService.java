package com.example.inventory.service;

import com.example.inventory.model.Task;
import com.example.inventory.model.dto.TaskDTO;

import java.util.List;

public interface TaskService {
    public Task createTask(TaskDTO taskDTO);
}
