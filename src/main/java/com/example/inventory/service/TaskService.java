package com.example.inventory.service;

import com.example.inventory.model.Task;
import com.example.inventory.model.dto.ShowTask;
import com.example.inventory.model.dto.TaskDTO;

import java.util.List;

public interface TaskService {
    public Task createTask(TaskDTO taskDTO);
    public void deleteTask(Long id);
    public List<ShowTask> listTask(Long user);
}
