package com.example.inventory.model.dto;

import com.example.inventory.model.entityEnum.Role;
import lombok.Data;

@Data
public class UserProfileDTO {
    private Long id;
    private String username;
    private String email;
    private Role role; // Trả về ADMIN, OWNER, VIEWER...
}
