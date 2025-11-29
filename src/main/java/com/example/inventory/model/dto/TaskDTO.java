package com.example.inventory.model.dto;

import com.example.inventory.model.GroupTodo;
import com.example.inventory.model.Users;
import com.example.inventory.model.entityEnum.Priority;
import com.example.inventory.model.entityEnum.Status;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskDTO {
    private long id;
    private String title;
    private String description;
    private LocalDateTime deadline;
    private LocalDateTime creatAt;
    private LocalDateTime updatedAt;
    private Long userId;
    private Status status;
    private Priority priority;
    private Long groupId;

    // Dùng để HIỂN THỊ (Output) - Trả về danh sách object {id, name}
    private List<AssigneeDTO> assignees;

    // Dùng để NHẬP LIỆU (Input) - Nhận về danh sách ID [1, 2, 3]
    private List<Long> assigneeIds;

    @Data
    public static class AssigneeDTO {
        private Long userId;
        private String username;
    }

}
