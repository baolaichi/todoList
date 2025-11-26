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
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ChatServiceImpl implements ChatService {
    @Autowired
    private ChatMessageRepository chatMessageRepository;
    @Autowired
    private GroupRepository groupRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private GroupMemberRepository memberRepository;
    @Autowired
    private SimpMessagingTemplate messagingTemplate; // WebSocket tool

    @Override
    @Transactional
    public ChatMessage sendMessage(ChatMessageDTO dto, String username) {
        // 1. Check quyền thành viên
        boolean isMember = memberRepository.existsByGroupsId_IdAndUser_Username(dto.getGroupId(), username);
        if (!isMember) {
            throw new RuntimeException("Bạn không phải thành viên nhóm này, không thể gửi tin nhắn!");
        }

        // 2. Lấy thông tin người gửi và nhóm
        Users sender = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        GroupTodo groupTodo = groupRepository.findById(dto.getGroupId())
                .orElseThrow(() -> new RuntimeException("Group not found"));

        // 3. Tạo và lưu tin nhắn
        ChatMessage message = new ChatMessage();
        message.setContent(dto.getContent());
        message.setSender(sender);
        message.setGroup(groupTodo);
        message.setSentAt(LocalDateTime.now());

        ChatMessage savedMsg = chatMessageRepository.save(message);

        // 4. Bắn WebSocket (Realtime) ngay tại Service
        messagingTemplate.convertAndSend("/topic/group/" + dto.getGroupId(), savedMsg);

        return savedMsg;
    }

    @Override
    public List<ChatMessage> getChatHistory(Long groupId, String username) {
        // 1. BẢO MẬT: Kiểm tra xem user có phải thành viên nhóm không?
        // (Dùng hàm exists mà bạn đã sửa tên đúng ở các bước trước)
        boolean isMember = memberRepository.existsByGroupsId_IdAndUser_Username(groupId, username);

        if (!isMember) {
            throw new RuntimeException("Bạn không phải thành viên nhóm này, không được xem tin nhắn!");
        }

        // 2. Nếu đúng là thành viên -> Trả về list tin nhắn
        // Lưu ý: Nếu API trả về lỗi vòng lặp JSON, bạn nên convert sang DTO ở đây
        return chatMessageRepository.findByGroup_IdOrderBySentAtAsc(groupId);
    }

    @Override
    public List<ChatMessage> getGroupChatHistory(Long groupId, String username) {
        // 1. Check xem user có phải thành viên nhóm không? (Bảo mật)
        if (!memberRepository.existsByGroupsId_IdAndUser_Username(groupId, username)) {
            throw new RuntimeException("Bạn không phải thành viên nhóm này!");
        }
        return chatMessageRepository.findByGroup_IdOrderBySentAtAsc(groupId);
    }

}
