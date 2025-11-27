package com.example.inventory.model.dto;

import com.example.inventory.model.entityEnum.GroupRole;
import lombok.Data;

@Data
public class GroupMemberDTO {
    private Long userId;
    private String username;
    private String email;
    private GroupRole role; // LEADER hoặc MEMBER
}
