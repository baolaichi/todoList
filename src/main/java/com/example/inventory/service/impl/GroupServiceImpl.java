package com.example.inventory.service.impl;

import com.example.inventory.model.*;
import com.example.inventory.model.dto.*;
import com.example.inventory.model.entityEnum.GroupRole;
import com.example.inventory.model.entityEnum.Status;
import com.example.inventory.repository.*;
import com.example.inventory.service.GroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GroupServiceImpl implements GroupService {

    @Autowired private GroupRepository groupRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private GroupMemberRepository groupMemberRepository; // Đổi tên biến cho rõ nghĩa
    @Autowired private TaskRepository taskRepository;

    // 1. TẠO NHÓM
    @Override
    @Transactional
    public GroupDTO createGroup(CreateGroupDTO createDto, String username) {
        Users creator = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        GroupTodo group = new GroupTodo();
        group.setName(createDto.getName());
        group.setDescription(createDto.getDescription());
        group.setCreatedBy(creator);
        group.setCreatedAt(LocalDateTime.now());

        GroupTodo savedGroup = groupRepository.save(group);

        // Set người tạo làm LEADER
        GroupMember leaderMember = new GroupMember();
        leaderMember.setGroupsId(savedGroup); // Lưu ý: Tên biến trong Entity GroupMember là 'groupsId'
        leaderMember.setUser(creator);
        leaderMember.setRole(GroupRole.LEADER);
        groupMemberRepository.save(leaderMember);

        return mapToGroupDTO(savedGroup, GroupRole.LEADER);
    }

    // 2. LẤY DANH SÁCH NHÓM CỦA TÔI
    @Override
    public List<GroupDTO> getMyGroups(String username) {
        List<GroupMember> memberships = groupMemberRepository.findByUser_Username(username);
        return memberships.stream().map(member ->
                mapToGroupDTO(member.getGroupsId(), member.getRole())
        ).collect(Collectors.toList());
    }

    // 3. LẤY CHI TIẾT 1 NHÓM
    @Override
    public GroupDTO getGroupDetail(Long groupId, String username) {
        GroupMember membership = groupMemberRepository.findByGroupsId_IdAndUser_Username(groupId, username)
                .orElseThrow(() -> new RuntimeException("Bạn không phải thành viên hoặc nhóm không tồn tại"));

        return mapToGroupDTO(membership.getGroupsId(), membership.getRole());
    }

    // 4. THÊM THÀNH VIÊN
    @Override
    @Transactional
    public void addMember(Long groupId, AddMemberRequest request, String requesterUsername) {
        // Check quyền Leader
        if (!isLeader(groupId, requesterUsername)) {
            throw new RuntimeException("Chỉ Trưởng nhóm mới được thêm thành viên!");
        }

        // Tìm user cần thêm (bằng email hoặc username)
        Users userToAdd = userRepository.findByUsername(request.getEmailOrUsername())
                .or(() -> userRepository.findByEmail(request.getEmailOrUsername()))
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng: " + request.getEmailOrUsername()));

        // Check đã tồn tại chưa
        if (groupMemberRepository.existsByGroupsId_IdAndUser_Id(groupId, userToAdd.getId())) {
            throw new RuntimeException("Thành viên này đã có trong nhóm!");
        }

        GroupTodo group = groupRepository.findById(groupId).orElseThrow();
        GroupMember newMember = new GroupMember();
        newMember.setGroupsId(group);
        newMember.setUser(userToAdd);
        newMember.setRole(GroupRole.MEMBER);
        groupMemberRepository.save(newMember);
    }

    // 5. LẤY DANH SÁCH THÀNH VIÊN
    @Override
    public List<GroupMemberDTO> getGroupMembers(Long groupId, String username) {
        // Check quyền xem (User phải thuộc nhóm)
        if (!groupMemberRepository.existsByGroupsId_IdAndUser_Username(groupId, username)) {
            throw new RuntimeException("Bạn không có quyền xem thành viên nhóm này");
        }

        List<GroupMember> members = groupMemberRepository.findByGroupsId_Id(groupId);

        return members.stream().map(m -> {
            GroupMemberDTO dto = new GroupMemberDTO();
            dto.setUserId(m.getUser().getId());
            dto.setUsername(m.getUser().getUsername());
            dto.setEmail(m.getUser().getEmail());
            dto.setRole(m.getRole());
            return dto;
        }).collect(Collectors.toList());
    }

    // 6. TẠO TASK TRONG NHÓM
    @Override
    @Transactional
    public Task createTaskInGroup(TaskGroupDTO dto, String requesterUsername) {
        if (!isLeader(dto.getGroupId(), requesterUsername)) {
            throw new RuntimeException("Chỉ Trưởng nhóm mới được tạo công việc!");
        }

        if (!groupMemberRepository.existsByGroupsId_IdAndUser_Id(dto.getGroupId(), dto.getAssignToUserId())) {
            throw new RuntimeException("Người được giao không phải thành viên nhóm!");
        }

        GroupTodo group = groupRepository.findById(dto.getGroupId()).orElseThrow();
        Users assignee = userRepository.findById(dto.getAssignToUserId()).orElseThrow();

        Task task = new Task();
        task.setTitle(dto.getTitle());
        task.setDescription(dto.getDescription());
        task.setStatus(Status.TODO);
        task.setPriority(dto.getPriority());
        task.setCreatAt(LocalDateTime.now());

        if (dto.getDeadline() != null) {
            task.setDeadline(dto.getDeadline());
        }

        task.setGroup(group);
        task.setUser(assignee);

        return taskRepository.save(task);
    }

    // 7. CẬP NHẬT TASK TRONG NHÓM
    @Override
    @Transactional
    public Task updateTaskInGroup(Long taskId, TaskDTO dto, String requesterUsername) {
        Task task = taskRepository.findById(taskId).orElseThrow(() -> new RuntimeException("Task not found"));
        Long groupId = task.getGroup().getId();

        boolean isLeader = isLeader(groupId, requesterUsername);
        boolean isAssignee = task.getUser().getUsername().equals(requesterUsername);

        if (isLeader) {
            // Leader được sửa tất cả
            task.setTitle(dto.getTitle());
            task.setDescription(dto.getDescription());
            task.setPriority(dto.getPriority());
            if (dto.getDeadline() != null) task.setDeadline(dto.getDeadline());
            task.setStatus(dto.getStatus());
        } else if (isAssignee) {
            // Member chỉ được sửa trạng thái
            task.setStatus(dto.getStatus());
        } else {
            throw new RuntimeException("Bạn không có quyền chỉnh sửa task này!");
        }

        task.setUpdatedAt(LocalDateTime.now());
        return taskRepository.save(task);
    }

    // 8. LẤY DANH SÁCH TASK CỦA NHÓM
    @Override
    public List<TaskDTO> getTasksByGroupId(Long groupId, String username) {

        if (!groupMemberRepository.existsByGroupsId_IdAndUser_Username(groupId, username)) {
            throw new RuntimeException("Bạn không phải thành viên nhóm này!");
        }

        List<Task> tasks = taskRepository.findByGroup_Id(groupId);

        return tasks.stream().map(task -> {
            TaskDTO dto = new TaskDTO();
            dto.setId(task.getId());
            dto.setTitle(task.getTitle());
            dto.setDescription(task.getDescription());
            dto.setStatus(task.getStatus());
            dto.setPriority(task.getPriority());
            dto.setCreatAt(task.getCreatAt());
            if(task.getDeadline() != null) dto.setDeadline(task.getDeadline());

            dto.setUserId(task.getUser().getId());
            dto.setGroupId(task.getGroup().getId());

            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteTaskInGroup(Long taskId, String username) {
        Task task = taskRepository.findById(taskId).orElseThrow(() -> new RuntimeException("Task not found"));
        Long groupId = task.getGroup().getId();

        // Chỉ Leader mới được xóa task trong nhóm
        if (!isLeader(groupId, username)) {
            throw new RuntimeException("Chỉ Trưởng nhóm mới được xóa công việc!");
        }
        taskRepository.delete(task);
    }

    // --- Helper Methods ---
    private boolean isLeader(Long groupId, String username) {
        return groupMemberRepository.findByGroupsId_IdAndUser_Username(groupId, username)
                .map(m -> m.getRole() == GroupRole.LEADER)
                .orElse(false);
    }

    private GroupDTO mapToGroupDTO(GroupTodo group, GroupRole myRole) {
        GroupDTO dto = new GroupDTO();
        dto.setId(group.getId());
        dto.setName(group.getName());
        dto.setDescription(group.getDescription());
        dto.setCreatedAt(group.getCreatedAt());
        // Member count lấy từ @Formula trong Entity
        dto.setMemberCount(group.getMemberCount());
        dto.setMyRole(myRole.name());
        return dto;
    }
}