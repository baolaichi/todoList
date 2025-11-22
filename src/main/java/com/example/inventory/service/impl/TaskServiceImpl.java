package com.example.inventory.service.impl;

import com.example.inventory.model.Task;
import com.example.inventory.model.Users;
import com.example.inventory.model.dto.ShowTask;
import com.example.inventory.model.dto.TaskDTO;
import com.example.inventory.repository.TaskRepository;
import com.example.inventory.repository.UserRepository;
import com.example.inventory.service.TaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

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

    @Override
    public void deleteTask(Long id){

        if (!taskRepository.existsById(id)){
            throw  new RuntimeException("không tìm thấy Task: " + id);

        }
        taskRepository.deleteById(id);
    }

    @Override
    public List<ShowTask> listTask(Long userId){
        try {
            List<Task> tasks = taskRepository.findByUser_Id(userId);
            return tasks.stream()
                    .map(t -> new ShowTask(t.getTitle(), t.getDeadline(), t.getCreatAt()))
                    .collect(Collectors.toList());
        }catch (RuntimeException e){
            throw new RuntimeException("user không có Task");
        }
    }
}
