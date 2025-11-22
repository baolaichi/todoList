package com.example.inventory.controller;

import com.example.inventory.model.Task;
import com.example.inventory.model.Users;
import com.example.inventory.model.dto.ShowTask;
import com.example.inventory.model.dto.TaskDTO;
import com.example.inventory.repository.UserRepository;
import com.example.inventory.service.TaskService;
import org.springframework.boot.autoconfigure.neo4j.Neo4jProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;


import java.util.List;

@RestController
@RequestMapping("/api/user/tasks")
public class TaskController {
    private final TaskService taskService;
    private final UserRepository userRepository;

    public TaskController(TaskService taskService, UserRepository userRepository) {
        this.taskService = taskService;
        this.userRepository = userRepository;
    }

    @PostMapping("/add")
    public ResponseEntity<Task> createNewTask(@RequestBody TaskDTO taskDTO){
        try {
            Task createTask = taskService.createTask(taskDTO);
            return new ResponseEntity<>(createTask, HttpStatus.CREATED);
        }catch (RuntimeException e){
            return new ResponseEntity(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteTask(@PathVariable long id){
        try {
            taskService.deleteTask(id);
            return ResponseEntity.ok("Đã xóa thành công: " + id);
        }catch (RuntimeException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/show")
        public ResponseEntity<List<ShowTask>> getAllListForUser(Authentication authentication){
            try {
                String username = authentication.getName();

                Users users = userRepository.findByUsername(username)
                                .orElseThrow(() -> new RuntimeException("User not found"));
                List<ShowTask> list = taskService.listTask(users.getId());
                return ResponseEntity.ok(list);
            }catch (RuntimeException e){
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(List.of());
            }
    }


}
