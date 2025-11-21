package com.example.inventory.service.impl;

import com.example.inventory.model.Task;
import com.example.inventory.model.Users;
import com.example.inventory.model.dto.TaskDTO;
import com.example.inventory.repository.TaskRepository;
import com.example.inventory.repository.UserRepository;
import com.example.inventory.service.TaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TaskServiceImpl implements TaskService {
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public TaskServiceImpl(TaskRepository taskRepository, UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    @Override
    public Task createTask(TaskDTO taskDTO){
        Users users = userRepository.findById(taskDTO.getUserId())
                .orElseThrow(() -> new RuntimeException("Không có người dùng"));

        Task newTask = new Task();
        newTask.setTitle(taskDTO.getTitle());
        newTask.setDescription(taskDTO.getDescription());
        newTask.setStatus(taskDTO.getStatus());
        newTask.setDeadline(taskDTO.getDeadline());
        newTask.setCreatAt(taskDTO.getCreatAt());
        newTask.setPriority(taskDTO.getPriority());
        newTask.setUser(users);

        return taskRepository.save(newTask);
    }
}
