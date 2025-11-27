package com.example.inventory.service;

import com.example.inventory.model.ChatMessage;
import com.example.inventory.model.dto.ChatMessageDTO;

import java.util.List;

public interface ChatService {
    ChatMessage sendMessage(ChatMessageDTO dto, String username);

    // Hàm lấy lịch sử
    List<ChatMessage> getChatHistory(Long groupId, String username);
}
