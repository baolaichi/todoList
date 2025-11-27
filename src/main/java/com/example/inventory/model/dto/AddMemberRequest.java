package com.example.inventory.model.dto;

import lombok.Data;

@Data
public class AddMemberRequest {
    /**
     * Chuỗi này có thể là Username hoặc Email.
     * Service sẽ tự động kiểm tra xem nó khớp với username hay email trong database.
     */
    private String emailOrUsername;
}