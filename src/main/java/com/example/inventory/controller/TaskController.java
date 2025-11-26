package com.example.inventory.controller;

import com.example.inventory.model.Task;
import com.example.inventory.model.Users;
import com.example.inventory.model.dto.ShowTask;
import com.example.inventory.model.dto.TaskDTO;
import com.example.inventory.model.entityEnum.Status;
import com.example.inventory.repository.UserRepository;
import com.example.inventory.service.TaskService;
import org.springframework.boot.autoconfigure.neo4j.Neo4jProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;


import java.util.List;
import java.util.Map;

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
            String username = SecurityContextHolder.getContext().getAuthentication().getName();

            Task createTask = taskService.createTask(taskDTO, username);
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

    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateTask(@PathVariable long id, @RequestBody TaskDTO taskDTO){
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        Task updateTask = taskService.updatePersonalTask(id, taskDTO, username);
        return ResponseEntity.ok(updateTask);
    }

    @GetMapping("/show")
    public ResponseEntity<List<ShowTask>> getAllListForUser(Authentication authentication) {
        // Lấy username từ Spring Security
        String username = authentication.getName();

        // Gọi Service (Logic tìm user và convert nằm hết bên trong)
        List<ShowTask> list = taskService.getAllTasksForUser(username);

        // Luôn trả về 200 OK (kể cả khi list rỗng)
        return ResponseEntity.ok(list);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable long id, @RequestBody Map<String, String> requestBody){
        try {
            Status newStatus;
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            String statusStr = requestBody.get("status");
            if (statusStr == null){
                return ResponseEntity.badRequest().body("Vui lòng gửi lại trạng thái");
            }
            try {
                newStatus = Status.valueOf(statusStr.toUpperCase());
            }catch (IllegalArgumentException e){
                return ResponseEntity.badRequest().body("Trạng thái không hợp lệ! ");
            }
            Task task = taskService.statusTask(id, username, newStatus);
            return ResponseEntity.ok("Cập nhập status Task: " + newStatus);
        }catch (RuntimeException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/filter")
    public ResponseEntity<?> filterTasks(@RequestParam String type) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            List<ShowTask> tasks = taskService.filterTasksByDeadline(type, username);

            return ResponseEntity.ok(tasks);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

}
