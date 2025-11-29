package com.example.inventory.service.impl;

import com.example.inventory.model.*;
import com.example.inventory.model.dto.*;
import com.example.inventory.model.entityEnum.GroupRole;
import com.example.inventory.model.entityEnum.Status;
import com.example.inventory.repository.*;
import com.example.inventory.service.GroupService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GroupServiceImpl implements GroupService {

    @Autowired private GroupRepository groupRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private GroupMemberRepository groupMemberRepository;
    @Autowired private TaskGroupRepository taskGroupRepository;
    @Autowired private TaskRepository taskRepository; // Giữ lại để tương thích ngược
    @Autowired private WorkLogRepository workLogRepository;

    // --- PHẦN FILE & FOLDER (Chuyển từ FolderServiceImpl sang đây nếu muốn gộp, hoặc giữ nguyên bên kia) ---
    // Ở đây tôi giả định bạn đang dùng FolderServiceImpl riêng cho file/folder.
    // Nếu bạn muốn gộp hết vào đây thì báo tôi nhé.
    // Dưới đây là các hàm logic chính của GroupService.

    // 1. TẠO NHÓM
    @Override
    @Transactional
    public GroupDTO createGroup(CreateGroupDTO createDto, String username) {
        Users creator = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        GroupTodo group = new GroupTodo();
        group.setName(createDto.getName());
        group.setDescription(createDto.getDescription());
        group.setCreatedBy(creator);
        group.setCreatedAt(LocalDateTime.now());

        GroupTodo savedGroup = groupRepository.save(group);

        GroupMember leaderMember = new GroupMember();
        leaderMember.setGroupsId(savedGroup);
        leaderMember.setUser(creator);
        leaderMember.setRole(GroupRole.LEADER);
        groupMemberRepository.save(leaderMember);

        GroupDTO dto = mapToGroupDTO(savedGroup, GroupRole.LEADER);
        dto.setMemberCount(1);
        return dto;
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
        if (!isLeader(groupId, requesterUsername)) {
            throw new RuntimeException("Chỉ Trưởng nhóm mới được thêm thành viên!");
        }

        Users userToAdd = userRepository.findByUsername(request.getEmailOrUsername())
                .or(() -> userRepository.findByEmail(request.getEmailOrUsername()))
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng: " + request.getEmailOrUsername()));

        if (groupMemberRepository.existsByGroupsId_IdAndUser_Id(groupId, userToAdd.getId())) {
            throw new RuntimeException("Thành viên này đã có trong nhóm!");
        }

        GroupTodo group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Nhóm không tồn tại"));

        GroupMember newMember = new GroupMember();
        newMember.setGroupsId(group);
        newMember.setUser(userToAdd);
        newMember.setRole(GroupRole.MEMBER);
        groupMemberRepository.save(newMember);
    }

    // 5. LẤY DANH SÁCH THÀNH VIÊN
    @Override
    public List<GroupMemberDTO> getGroupMembers(Long groupId, String username) {
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

    // 6. TẠO TASK TRONG NHÓM (DÙNG TASK GROUP)
// --- 6. TẠO TASK (DÙNG TASK GROUP DTO) ---
    @Override
    @Transactional
    public TaskGroupDTO createTaskInGroup(TaskGroupDTO dto, String requesterUsername) {
        if (!isLeader(dto.getGroupId(), requesterUsername)) {
            throw new RuntimeException("Chỉ Trưởng nhóm mới được tạo công việc!");
        }
        if (dto.getAssigneeIds() == null || dto.getAssigneeIds().isEmpty()) {
            throw new RuntimeException("Phải chọn ít nhất 1 người thực hiện!");
        }

        GroupTodo group = groupRepository.findById(dto.getGroupId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhóm!"));

        List<Users> assigneesList = userRepository.findAllById(dto.getAssigneeIds());
        if (assigneesList.isEmpty()) throw new RuntimeException("Danh sách người được giao không hợp lệ!");

        TaskGroup task = new TaskGroup();
        task.setTitle(dto.getTitle());
        task.setDescription(dto.getDescription());
        task.setStatus(dto.getStatus() != null ? dto.getStatus() : Status.TODO);
        task.setPriority(dto.getPriority());
        task.setCreatedAt(LocalDateTime.now());

        // Parse Deadline String -> LocalDateTime
        if (dto.getDeadline() != null && !dto.getDeadline().isEmpty()) {
            try {
                task.setDeadline(LocalDateTime.parse(dto.getDeadline(), DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            } catch (Exception e) {
                try {
                    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                    task.setDeadline(LocalDateTime.parse(dto.getDeadline(), fmt));
                } catch (Exception ex) {
                    System.err.println("Lỗi date: " + dto.getDeadline());
                }
            }
        }

        task.setGroup(group);
        Users creator = userRepository.findByUsername(requesterUsername).orElseThrow();
        task.setCreatedBy(creator);
        task.setAssignees(new HashSet<>(assigneesList));

        TaskGroup savedTask = taskGroupRepository.save(task);
        return mapToTaskGroupDTO(savedTask); // Trả về DTO riêng
    }

    // --- 7. UPDATE TASK (DÙNG TASK GROUP DTO) ---
    @Override
    @Transactional
    public TaskGroupDTO updateTaskInGroup(Long taskId, TaskGroupDTO dto, String requesterUsername) {
        TaskGroup task = taskGroupRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        boolean isLeader = isLeader(task.getGroup().getId(), requesterUsername);
        boolean isAssignee = task.getAssignees().stream().anyMatch(u -> u.getUsername().equals(requesterUsername));

        if (isLeader) {
            task.setTitle(dto.getTitle());
            task.setDescription(dto.getDescription());
            task.setPriority(dto.getPriority());
            task.setStatus(dto.getStatus());

            // Parse Deadline
            if (dto.getDeadline() != null && !dto.getDeadline().isEmpty()) {
                try {
                    task.setDeadline(LocalDateTime.parse(dto.getDeadline(), DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                } catch (Exception e) {
                    try {
                        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                        task.setDeadline(LocalDateTime.parse(dto.getDeadline(), fmt));
                    } catch (Exception ex) {}
                }
            }

            // Update Assignees
            if (dto.getAssigneeIds() != null) {
                if (dto.getAssigneeIds().isEmpty()) task.getAssignees().clear();
                else {
                    List<Users> newUsers = userRepository.findAllById(dto.getAssigneeIds());
                    task.setAssignees(new HashSet<>(newUsers));
                }
            }
        } else if (isAssignee) {
            task.setStatus(dto.getStatus());
        } else {
            throw new RuntimeException("Không có quyền sửa!");
        }

        task.setUpdatedAt(LocalDateTime.now());
        TaskGroup updatedTask = taskGroupRepository.save(task);
        return mapToTaskGroupDTO(updatedTask);
    }

    // --- 8. LẤY DANH SÁCH (TRẢ VỀ TASK GROUP DTO) ---
    @Override
    @Transactional(readOnly = true)
    public List<TaskGroupDTO> getTasksByGroupId(Long groupId, String username) {
        if (!groupMemberRepository.existsByGroupsId_IdAndUser_Username(groupId, username)) {
            throw new RuntimeException("Bạn không phải thành viên nhóm này!");
        }

        List<TaskGroup> tasks = taskGroupRepository.findByGroup_IdOrderByCreatedAtDesc(groupId);
        return tasks.stream().map(this::mapToTaskGroupDTO).collect(Collectors.toList());
    }

    // --- HELPER MAPPER RIÊNG CHO TASK GROUP ---
    private TaskGroupDTO mapToTaskGroupDTO(TaskGroup task) {
        TaskGroupDTO dto = new TaskGroupDTO();
        dto.setId(task.getId());
        dto.setTitle(task.getTitle());
        dto.setDescription(task.getDescription());
        dto.setStatus(task.getStatus());
        dto.setPriority(task.getPriority());
        dto.setCreatedAt(task.getCreatedAt());
        dto.setUpdatedAt(task.getUpdatedAt());

        // Convert Date -> String
        if (task.getDeadline() != null) {
            dto.setDeadline(task.getDeadline().toString());
        }

        dto.setGroupId(task.getGroup().getId());

        // Map Assignees
        if (task.getAssignees() != null) {
            List<TaskGroupDTO.AssigneeDTO> assigneeDTOS = task.getAssignees().stream().map(u ->
                    new TaskGroupDTO.AssigneeDTO(u.getId(), u.getUsername())
            ).collect(Collectors.toList());
            dto.setAssignees(assigneeDTOS);
        }
        return dto;
    }

    // 9. XÓA TASK NHÓM
    @Override
    @Transactional
    public void deleteTaskInGroup(Long taskId, String requesterUsername) {
        TaskGroup taskGroup = taskGroupRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        if (!isLeader(taskGroup.getGroup().getId(), requesterUsername)) {
            throw new RuntimeException("Chỉ Trưởng nhóm mới được xóa công việc!");
        }
        taskGroupRepository.delete(taskGroup);
    }

    // 10. BÁO CÁO TIẾN ĐỘ
    @Override
    @Transactional
    public void createWorkLog(Long taskId, String content, String username) {
        TaskGroup taskGroup = taskGroupRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        Users reporter = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check quyền: Leader HOẶC Người được giao việc (Assignees)
        boolean isLeader = isLeader(taskGroup.getGroup().getId(), username);
        boolean isAssignee = taskGroup.getAssignees().stream()
                .anyMatch(u -> u.getUsername().equals(username));

        if (!isLeader && !isAssignee) {
            throw new RuntimeException("Bạn không tham gia task này, không thể báo cáo!");
        }

        WorkLog log = new WorkLog();
        log.setContent(content);
        log.setTaskGroup(taskGroup);
        log.setReporter(reporter);
        log.setCreatedAt(LocalDateTime.now());

        workLogRepository.save(log);
    }

    // 11. LẤY BÁO CÁO NGÀY
    @Override
    public List<WorkLogDTO> getDailyReports(Long groupId) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDateTime.now();

        List<WorkLog> logs = workLogRepository.findDailyReports(groupId, startOfDay, endOfDay);

        return logs.stream().map(this::mapLogToDTO).collect(Collectors.toList());
    }

    // 12. LẤY LỊCH SỬ BÁO CÁO CỦA 1 TASK
    @Override
    public List<WorkLogDTO> getWorkLogs(Long taskId, String username) {
        TaskGroup taskGroup = taskGroupRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        // Check quyền xem: Phải là Leader HOẶC Assignee
        boolean isLeader = isLeader(taskGroup.getGroup().getId(), username);

        // Kiểm tra xem user có nằm trong danh sách được giao không
        boolean isAssignee = taskGroup.getAssignees().stream()
                .anyMatch(u -> u.getUsername().equals(username));

        // --- SỬA: BỎ COMMENT ĐỂ KÍCH HOẠT BẢO MẬT ---
        if (!isLeader && !isAssignee) {
            throw new RuntimeException("Bạn không có quyền xem báo cáo của task này (Chỉ người được giao hoặc Leader)!");
        }
        // ---------------------------------------------

        return workLogRepository.findByTaskGroup_IdOrderByCreatedAtDesc(taskId).stream()
                .map(this::mapLogToDTO).collect(Collectors.toList());
    }

    // --- HELPER METHODS ---
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
        dto.setMemberCount(group.getMemberCount());
        dto.setMyRole(myRole.name());
        return dto;
    }

    private Task convertToOldTask(TaskGroup tg) {
        Task t = new Task();
        t.setId(tg.getId());
        t.setTitle(tg.getTitle());
        t.setStatus(tg.getStatus());
        // map thêm nếu cần
        return t;
    }

    private WorkLogDTO mapLogToDTO(WorkLog log) {
        WorkLogDTO dto = new WorkLogDTO();
        dto.setId(log.getId());
        dto.setContent(log.getContent());
        dto.setCreatedAt(log.getCreatedAt());
        dto.setReporterName(log.getReporter().getUsername());
        if (log.getTaskGroup() != null) {
            dto.setTaskTitle(log.getTaskGroup().getTitle());
            dto.setTaskId(log.getTaskGroup().getId());
        }
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public TaskDTO getTaskGroupDetail(Long taskId, String username) {
        TaskGroup taskGroup = taskGroupRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task Group not found ID: " + taskId));

        boolean isLeader = isLeader(taskGroup.getGroup().getId(), username);
        boolean isAssignee = taskGroup.getAssignees().stream().anyMatch(u -> u.getUsername().equals(username));

        // Cho phép thành viên trong nhóm xem chi tiết (để biết tiến độ),
        // hoặc giới hạn chỉ Leader/Assignee tùy bạn. Ở đây tôi mở cho cả nhóm xem được.
        if (!groupMemberRepository.existsByGroupsId_IdAndUser_Username(taskGroup.getGroup().getId(), username)) {
            throw new RuntimeException("Bạn không phải thành viên nhóm này!");
        }

        // Convert sang TaskDTO để Frontend hiển thị
        TaskDTO dto = new TaskDTO();
        dto.setId(taskGroup.getId());
        dto.setTitle(taskGroup.getTitle());
        dto.setDescription(taskGroup.getDescription());
        dto.setStatus(taskGroup.getStatus());
        dto.setPriority(taskGroup.getPriority());
        dto.setCreatAt(taskGroup.getCreatedAt());
        dto.setUpdatedAt(taskGroup.getUpdatedAt());

        if (taskGroup.getDeadline() != null) {
            dto.setDeadline(LocalDateTime.parse(taskGroup.getDeadline().toString()));
        }

        dto.setGroupId(taskGroup.getGroup().getId());
        if (taskGroup.getCreatedBy() != null) {
            dto.setUserId(taskGroup.getCreatedBy().getId());
        }

        // Map Assignees (Quan trọng để Frontend biết ai làm mà hiện nút Báo cáo)
        if (taskGroup.getAssignees() != null) {
            List<TaskDTO.AssigneeDTO> assigneeDTOS = taskGroup.getAssignees().stream().map(u -> {
                TaskDTO.AssigneeDTO a = new TaskDTO.AssigneeDTO();
                a.setUserId(u.getId());
                a.setUsername(u.getUsername());
                return a;
            }).collect(Collectors.toList());
            dto.setAssignees(assigneeDTOS);
        }

        return dto;
    }
}