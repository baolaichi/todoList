package com.example.inventory.service;

import com.example.inventory.model.Task;
import com.example.inventory.model.dto.ShowTask;
import com.example.inventory.model.dto.TaskDTO;
import com.example.inventory.model.entityEnum.Status;

import java.util.List;

public interface TaskService {
    public Task createTask(TaskDTO taskDTO, String username);
    public void deleteTask(Long id);
    public List<ShowTask> listTask(Long user);
    Task updatePersonalTask(Long taskId, TaskDTO taskDTO, String username);
    Task statusTask(Long taskId, String username, Status newStatus);
    public List<ShowTask> filterTasksByDeadline(String filterType, String username);
    List<ShowTask> getAllTasksForUser(String username);
}
