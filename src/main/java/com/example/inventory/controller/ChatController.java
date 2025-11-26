package com.example.inventory.controller;

import com.example.inventory.model.ChatMessage;
import com.example.inventory.model.GroupTodo;
import com.example.inventory.model.Users;
import com.example.inventory.model.dto.ChatMessageDTO;
import com.example.inventory.repository.ChatMessageRepository;
import com.example.inventory.repository.GroupMemberRepository;
import com.example.inventory.repository.GroupRepository;
import com.example.inventory.repository.UserRepository;
import com.example.inventory.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    @Autowired
    private ChatService chatService;

    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(@RequestBody ChatMessageDTO dto) {
        try {
            // Lấy username từ Token
            String username = SecurityContextHolder.getContext().getAuthentication().getName();

            // Gọi Service xử lý
            return ResponseEntity.ok(chatService.sendMessage(dto, username));

        } catch (RuntimeException e) {
            // Bắt lỗi từ Service ném ra (ví dụ lỗi không phải thành viên)
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    @GetMapping("/history/{groupId}")
    public ResponseEntity<?> getHistory(@PathVariable Long groupId) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();

            // Truyền username vào Service để check quyền:
            // "User này có thuộc Group này không?" -> Nếu không thì chặn.
            return ResponseEntity.ok(chatService.getChatHistory(groupId, username)); // Sửa Service nhận thêm tham số username

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }
}
