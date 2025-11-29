package com.example.inventory.model.dto;

import com.example.inventory.model.entityEnum.Priority;
import com.example.inventory.model.entityEnum.Status;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskGroupDTO {
    private Long id; // Thêm ID để dùng cho Update/Response

    private String title;
    private String description;

    // Dùng String để tránh lỗi format ngày tháng
    private String deadline;

    private LocalDateTime createdAt; // Thêm ngày tạo
    private LocalDateTime updatedAt; // Thêm ngày sửa

    private Priority priority;
    private Status status;

    private Long groupId;

    // Input: Danh sách ID để gửi lên khi Tạo/Sửa
    private List<Long> assigneeIds;

    // Output: Danh sách object để hiển thị Avatar
    private List<AssigneeDTO> assignees;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AssigneeDTO {
        private Long userId;
        private String username;
    }
}