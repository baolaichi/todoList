package com.example.inventory.service.impl;

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
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ChatServiceImpl implements ChatService {

    @Autowired private ChatMessageRepository chatMessageRepository;
    @Autowired private GroupRepository groupRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private GroupMemberRepository memberRepository;
    @Autowired private SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public ChatMessage sendMessage(ChatMessageDTO dto, String username) {
        // 1. Check quyền thành viên
        boolean isMember = memberRepository.existsByGroupsId_IdAndUser_Username(dto.getGroupId(), username);
        if (!isMember) {
            throw new RuntimeException("Bạn không phải thành viên nhóm này, không thể gửi tin nhắn!");
        }

        // 2. Lấy thông tin
        Users sender = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        GroupTodo groupTodo = groupRepository.findById(dto.getGroupId())
                .orElseThrow(() -> new RuntimeException("Group not found"));

        // 3. Lưu tin nhắn
        ChatMessage message = new ChatMessage();
        message.setContent(dto.getContent());
        message.setSender(sender);
        message.setGroup(groupTodo);
        message.setSentAt(LocalDateTime.now());

        ChatMessage savedMsg = chatMessageRepository.save(message);

        // 4. Bắn WebSocket
        messagingTemplate.convertAndSend("/topic/group/" + dto.getGroupId(), savedMsg);

        return savedMsg;
    }

    @Override
    public List<ChatMessage> getChatHistory(Long groupId, String username) {
        // 1. Check quyền xem
        if (!memberRepository.existsByGroupsId_IdAndUser_Username(groupId, username)) {
            throw new RuntimeException("Bạn không phải thành viên nhóm này, không được xem tin nhắn!");
        }

        // 2. Lấy lịch sử
        // Lưu ý: Thay 'Group' hoặc 'GroupsId' trong tên hàm repo tùy theo Entity ChatMessage của bạn
        return chatMessageRepository.findByGroup_IdOrderBySentAtAsc(groupId);
    }
}