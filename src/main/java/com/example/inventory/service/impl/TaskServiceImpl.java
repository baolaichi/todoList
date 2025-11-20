package com.example.inventory.service.impl;

import com.example.inventory.model.Task;
import com.example.inventory.repository.TaskRepository;
import com.example.inventory.service.TaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TaskServiceImpl implements TaskService {
    private final TaskRepository taskRepository;

    public TaskServiceImpl(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Override
    public Task createTask(){

        return taskRepository.save();
    }
}
