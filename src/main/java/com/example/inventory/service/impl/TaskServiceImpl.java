package com.example.inventory.service.impl;

import com.example.inventory.exception.ResourceNotFoundException;
import com.example.inventory.model.Task;
import com.example.inventory.model.Users;
import com.example.inventory.model.dto.ShowTask;
import com.example.inventory.model.dto.TaskDTO;
import com.example.inventory.model.entityEnum.Status;
import com.example.inventory.repository.TaskRepository;
import com.example.inventory.repository.UserRepository;
import com.example.inventory.service.TaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
    public Task createTask(TaskDTO taskDTO, String username){
        Users users = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không có người dùng"));

        Task newTask = new Task();
        newTask.setTitle(taskDTO.getTitle());
        newTask.setDescription(taskDTO.getDescription());
        newTask.setStatus(taskDTO.getStatus() != null ? taskDTO.getStatus() : Status.TODO);
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

    @Override
    public Task updatePersonalTask(Long taskId, TaskDTO taskDTO, String username){
        try {
            Task task = taskRepository.findById(taskId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy task với ID"));

            if(!task.getUser().getUsername().equals(username)){
                throw new RuntimeException(" Không có quyền sửa Task");
            }
            task.setTitle(taskDTO.getTitle());
            task.setDescription(taskDTO.getDescription());
            task.setStatus(taskDTO.getStatus());
            task.setPriority(taskDTO.getPriority());
            task.setDeadline(taskDTO.getDeadline());
            task.setUpdatedAt(taskDTO.getUpdatedAt());

            return taskRepository.save(task);
        }catch (RuntimeException e){
            throw new ResourceNotFoundException("Không tìm thấy task với ID: " + taskId);
        }
    }

    @Override
    public Task statusTask(Long taskId, String username, Status newStatus){
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy task với ID" + taskId));

        if(!task.getUser().getUsername().equals(username)){
            throw new ArithmeticException(username + "không được phép truy cập");
        }

        task.setStatus(newStatus);
        task.setUpdatedAt(LocalDateTime.now());

        return taskRepository.save(task);

    }

    @Override
    public List<ShowTask> filterTasksByDeadline(String filterType, String username) {

        // Khởi tạo List<Task> để giữ tính nhất quán trong khối switch
        List<Task> tasks;

        // Xử lý các trường hợp đặc biệt không dùng BETWEEN trước
        switch (filterType.toLowerCase()) {
            case "upcoming": // Sắp tới (Từ bây giờ trở đi)
                tasks = taskRepository.findByUser_UsernameAndDeadlineAfterOrderByDeadlineAsc(
                        username, LocalDateTime.now()
                );
                break;

            case "overdue": // Quá hạn (Nhỏ hơn hiện tại và chưa xong)
                tasks = taskRepository.findByUser_UsernameAndStatusNotAndDeadlineBefore(
                        username, Status.DONE, LocalDateTime.now() // Sử dụng Status.DONE
                );
                break;

            default:
                // Khối logic cho BETWEEN
                LocalDateTime start;
                LocalDateTime end;
                LocalDate today = LocalDate.now();

                switch (filterType.toLowerCase()) {
                    case "today": // Hôm nay
                        start = today.atStartOfDay();
                        end = today.atTime(LocalTime.MAX);
                        break;

                    case "tomorrow": // Ngày mai
                        LocalDate tomorrow = today.plusDays(1);
                        start = tomorrow.atStartOfDay();
                        end = tomorrow.atTime(LocalTime.MAX);
                        break;

                    case "this_week": // Tuần này (Thứ 2 -> Chủ nhật)
                        // Lưu ý: Java sử dụng ISO week, Thứ 2 là start, Chủ Nhật là end
                        start = today.with(java.time.DayOfWeek.MONDAY).atStartOfDay();
                        end = today.with(java.time.DayOfWeek.SUNDAY).atTime(LocalTime.MAX);
                        break;

                    default:
                        // 3. Sử dụng ngoại lệ chuẩn hơn
                        throw new IllegalArgumentException("Loại lọc không hợp lệ! Vui lòng chọn (today, tomorrow, this_week, upcoming, overdue)");
                }

                // Dùng phương thức BETWEEN chung cho các trường hợp còn lại
                tasks = taskRepository.findByUser_UsernameAndDeadlineBetweenOrderByDeadlineAsc(username, start, end);
                break;
        }

        // Map kết quả cuối cùng sang DTO (ShowTask)
        return tasks.stream()
                // 2. Kiểm tra lại lỗi chính tả: 'CreatAt' hay 'CreatedAt'
                .map(t -> new ShowTask(t.getTitle(), t.getDeadline(), t.getCreatAt()))
                .collect(Collectors.toList());
    }

    @Override
    public List<ShowTask> getAllTasksForUser(String username) {
        // 1. Tìm User (Service tự gọi Repo)
        Users user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 2. Lấy danh sách Task theo User ID (Sắp xếp mới nhất)
        // Giả sử Repo có hàm: findByUserIdOrderByCreatAtDesc(Long userId)
        List<Task> tasks = taskRepository.findByUser_IdOrderByCreatAtDesc(user.getId());

        // 3. Convert Entity -> ShowTask DTO
        return tasks.stream().map(task -> {
            ShowTask dto = new ShowTask();
            dto.setDeadline(task.getDeadline());
            dto.setTitle(task.getTitle());
            dto.setDeadline(task.getDeadline());
            return dto;
        }).toList();
    }
}
