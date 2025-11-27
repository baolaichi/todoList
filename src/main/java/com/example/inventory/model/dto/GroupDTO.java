package com.example.inventory.model.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class GroupDTO {
    private Long id;
    private String name;
    private String description;
    private int memberCount; // Số lượng thành viên trong nhóm
    private String myRole;   // Vai trò của người đang xem (LEADER hoặc MEMBER)
    private LocalDateTime createdAt;
}