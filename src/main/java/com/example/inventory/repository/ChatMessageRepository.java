package com.example.inventory.repository;

import com.example.inventory.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByGroup_IdOrderBySentAtAsc(Long groupId);

}
