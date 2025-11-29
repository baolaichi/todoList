package com.example.inventory.service.impl;

import com.example.inventory.exception.ResourceNotFoundException;
import com.example.inventory.model.Task;
import com.example.inventory.model.Users;
import com.example.inventory.model.dto.TaskDTO;
import com.example.inventory.model.entityEnum.Status;
import com.example.inventory.repository.TaskRepository;
import com.example.inventory.repository.UserRepository;
import com.example.inventory.service.TaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
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

    // --- 1. TẠO TASK CÁ NHÂN ---
    @Override
    @Transactional
    public Task createTask(TaskDTO taskDTO, String username){
        Users currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không có người dùng"));

        Task newTask = new Task();
        newTask.setTitle(taskDTO.getTitle());
        newTask.setDescription(taskDTO.getDescription());
        newTask.setStatus(taskDTO.getStatus() != null ? taskDTO.getStatus() : Status.TODO);

        // Xử lý Deadline (Nếu DTO là String thì parse, nếu là LocalDateTime thì gán thẳng)
        if (taskDTO.getDeadline() != null) {
            newTask.setDeadline(taskDTO.getDeadline());
        }

        newTask.setCreatAt(LocalDateTime.now());
        newTask.setPriority(taskDTO.getPriority());

        // QUAN TRỌNG: Chỉ set User (Chủ sở hữu), không có Assignees
        newTask.setUser(currentUser);

        return taskRepository.save(newTask);
    }

    // --- 2. XÓA TASK ---
    @Override
    @Transactional // Bắt buộc phải có Transactional cho thao tác Delete/Modifying
    public void deleteTask(Long id){
        if (!taskRepository.existsById(id)){
            throw new RuntimeException("Không tìm thấy Task: " + id);
        }

        // 1. Xóa dữ liệu trong bảng phụ trước (để tránh lỗi Foreign Key 1451)
        taskRepository.deleteAssigneesByTaskId(id); // Xóa assignees cũ
        taskRepository.deleteWorkLogsByTaskId(id);  // Xóa log báo cáo

        // 2. Sau đó mới xóa Task chính
        taskRepository.deleteById(id);
    }

    // --- 3. LIST TASK (Logic cũ) ---
    @Override
    public List<TaskDTO> listTask(Long userId){
        try {
            List<Task> tasks = taskRepository.findByUser_Id(userId);
            return tasks.stream().map(this::mapToDTO).collect(Collectors.toList());
        } catch (RuntimeException e){
            throw new RuntimeException("Lỗi lấy danh sách Task");
        }
    }

    // --- 4. CẬP NHẬT TASK CÁ NHÂN ---
    @Override
    @Transactional
    public Task updatePersonalTask(Long taskId, TaskDTO taskDTO, String username){
        try {
            Task task = taskRepository.findById(taskId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy task ID: " + taskId));

            // Chỉ chủ sở hữu mới được sửa
            if(!task.getUser().getUsername().equals(username)){
                throw new RuntimeException("Không có quyền sửa Task này");
            }

            task.setTitle(taskDTO.getTitle());
            task.setDescription(taskDTO.getDescription());
            task.setStatus(taskDTO.getStatus());
            task.setPriority(taskDTO.getPriority());

            if (taskDTO.getDeadline() != null) {
                task.setDeadline(taskDTO.getDeadline());
            }

            task.setUpdatedAt(LocalDateTime.now());

            return taskRepository.save(task);
        } catch (RuntimeException e){
            throw new ResourceNotFoundException("Lỗi cập nhật: " + e.getMessage());
        }
    }

    // --- 5. CẬP NHẬT TRẠNG THÁI ---
    @Override
    @Transactional
    public Task statusTask(Long taskId, String username, Status newStatus){
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy task ID: " + taskId));

        // Với Task cá nhân, chỉ chủ sở hữu mới được sửa
        if(!task.getUser().getUsername().equals(username)){
            throw new RuntimeException("Bạn không được phép cập nhật trạng thái task này");
        }

        task.setStatus(newStatus);
        task.setUpdatedAt(LocalDateTime.now());

        return taskRepository.save(task);
    }

    // --- 6. LỌC TASK ---
    @Override
    public List<TaskDTO> filterTasksByDeadline(String filterType, String username) {
        List<Task> tasks;
        LocalDateTime now = LocalDateTime.now();

        switch (filterType.toLowerCase()) {
            case "upcoming":
                tasks = taskRepository.findByUser_UsernameAndDeadlineAfterOrderByDeadlineAsc(username, now);
                break;
            case "overdue":
                tasks = taskRepository.findByUser_UsernameAndStatusNotAndDeadlineBefore(username, Status.DONE, now);
                break;
            default:
                LocalDateTime start;
                LocalDateTime end;
                LocalDate today = LocalDate.now();

                switch (filterType.toLowerCase()) {
                    case "today":
                        start = today.atStartOfDay(); end = today.atTime(LocalTime.MAX); break;
                    case "tomorrow":
                        LocalDate tomorrow = today.plusDays(1); start = tomorrow.atStartOfDay(); end = tomorrow.atTime(LocalTime.MAX); break;
                    case "this_week":
                        start = today.with(java.time.DayOfWeek.MONDAY).atStartOfDay(); end = today.with(java.time.DayOfWeek.SUNDAY).atTime(LocalTime.MAX); break;
                    default:
                        throw new IllegalArgumentException("Loại lọc không hợp lệ!");
                }
                tasks = taskRepository.findByUser_UsernameAndDeadlineBetweenOrderByDeadlineAsc(username, start, end);
                break;
        }

        return tasks.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    // --- 7. LẤY TẤT CẢ TASK CHO USER ---
    @Override
    public List<TaskDTO> getAllTasksForUser(String username) {
        Users user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Task> tasks = taskRepository.findByUser_IdOrderByCreatAtDesc(user.getId());

        return tasks.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    // --- 8. XEM CHI TIẾT TASK ---
    @Override
    @Transactional(readOnly = true)
    public TaskDTO getTaskDetail(Long taskId, String username) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy task ID: " + taskId));

        boolean isOwner = task.getUser().getUsername().equals(username);
        if (!isOwner) {
            // throw new RuntimeException("Không có quyền xem chi tiết");
        }

        return mapToDTO(task);
    }

    // --- HELPER: MAP ENTITY -> DTO ---
    private TaskDTO mapToDTO(Task task) {
        TaskDTO dto = new TaskDTO();
        dto.setId(task.getId());
        dto.setTitle(task.getTitle());
        dto.setDescription(task.getDescription());
        dto.setStatus(task.getStatus());
        dto.setPriority(task.getPriority());
        dto.setCreatAt(task.getCreatAt());
        dto.setUpdatedAt(task.getUpdatedAt());

        if (task.getDeadline() != null) {
            dto.setDeadline(task.getDeadline());
        }

        if (task.getUser() != null) {
            dto.setUserId(task.getUser().getId());

            // --- QUAN TRỌNG: GIẢ LẬP DANH SÁCH ASSIGNEES ---
            // Vì Frontend dùng chung component hiển thị, nên ta cần trả về
            // danh sách chứa chính chủ sở hữu để hiển thị Avatar.
            TaskDTO.AssigneeDTO owner = new TaskDTO.AssigneeDTO();
            owner.setUserId(task.getUser().getId());
            owner.setUsername(task.getUser().getUsername());
            dto.setAssignees(Collections.singletonList(owner));
            // -----------------------------------------------
        }

        return dto;
    }
}