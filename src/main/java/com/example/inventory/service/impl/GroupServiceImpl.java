package com.example.inventory.service.impl;

import com.example.inventory.model.GroupMember;
import com.example.inventory.model.GroupTodo;
import com.example.inventory.model.Task;
import com.example.inventory.model.Users;
import com.example.inventory.model.dto.CreateGroupDTO;
import com.example.inventory.model.dto.TaskGroupDTO;
import com.example.inventory.model.entityEnum.GroupRole;
import com.example.inventory.repository.GroupMemberRepository;
import com.example.inventory.repository.GroupRepository;
import com.example.inventory.repository.TaskRepository;
import com.example.inventory.repository.UserRepository;
import com.example.inventory.service.GroupService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class GroupServiceImpl implements GroupService {
    private final GroupRepository groupRepository;
    private final GroupMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;

    public GroupServiceImpl(GroupRepository groupRepository, GroupMemberRepository memberRepository, UserRepository userRepository, TaskRepository taskRepository) {
        this.groupRepository = groupRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
    }

    @Override
    @Transactional // <--- QUAN TRỌNG: Để đảm bảo lưu cả 2 bảng cùng lúc
    public GroupTodo createGroup(CreateGroupDTO groupDTO, String username) {

        // 1. Lấy thông tin người tạo (User đang đăng nhập)
        Users user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 2. Tạo và Lưu Nhóm
        GroupTodo group = new GroupTodo();
        group.setName(groupDTO.getName());
        group.setDescription(groupDTO.getDescription());
        group.setCreatedBy(user); // Lưu vết người tạo (để hiển thị)

        GroupTodo savedGroup = groupRepository.save(group); // Lưu nhóm để lấy ID

        // 3. --- QUAN TRỌNG: SET NGƯỜI TẠO LÀM TRƯỞNG NHÓM (LEADER) ---
        GroupMember leaderMember = new GroupMember();
        leaderMember.setGroupsId(savedGroup); // Link với nhóm vừa tạo
        leaderMember.setUser(user);        // Link với user đang tạo
        leaderMember.setRole(GroupRole.LEADER); // Cấp quyền cao nhất

        memberRepository.save(leaderMember); // Lưu vào bảng thành viên
        // -------------------------------------------------------------

        return savedGroup;
    }

    @Override
    public void addMember(Long groupId, Long userId, String requesterUsername) {

        if (!isLeader(groupId, requesterUsername)) {
            throw new RuntimeException("Chỉ trưởng nhóm mới được thêm thành viên!");
        }

        // Bước 2: Kiểm tra xem User cần thêm có tồn tại không?
        Users userToAdd = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy User với ID: " + userId));

        // Bước 3: Kiểm tra Group có tồn tại không?
        GroupTodo group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Group"));

        // Bước 4: (Quan trọng) Check xem user này ĐÃ Ở TRONG NHÓM CHƯA?
        if (memberRepository.existsByGroupsId_IdAndUser_Id(groupId, userId)) {
            throw new RuntimeException("Thành viên này đã có trong nhóm rồi!");
        }

        // Bước 5: Thêm thành viên mới (Role là MEMBER)
        GroupMember newMember = new GroupMember();
        newMember.setGroupsId(group);
        newMember.setUser(userToAdd);
        newMember.setRole(GroupRole.MEMBER); // Chỉ set là MEMBER thôi

        memberRepository.save(newMember);
    }

    @Override
    public Task createTaskInGroup(TaskGroupDTO dto, String requesterUsername) {
        // 1. Check quyền Trưởng nhóm
        if (!isLeader(dto.getGroupId(), requesterUsername)) {
            throw new RuntimeException("Chỉ trưởng nhóm mới được tạo task!");
        }

        // 2. Lấy thông tin Group và User (người được giao)
        GroupTodo groupTodo = groupRepository.findById(dto.getGroupId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Group"));

        Users assignee = userRepository.findById(dto.getAssignToUserId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thành viên để giao việc"));

        // 3. --- QUAN TRỌNG: Map dữ liệu từ DTO sang Entity ---
        Task task = new Task();
        task.setTitle(dto.getTitle());
        task.setDescription(dto.getDescription());
        task.setStatus(dto.getStatus());     // Enum Status
        task.setPriority(dto.getPriority()); // Enum Priority

        if (dto.getDeadline() != null) {
            task.setDeadline(dto.getDeadline());
        }

        task.setCreatAt(LocalDateTime.now()); // Set thời gian tạo là lúc này

        // 4. Gán quan hệ
        task.setGroup(groupTodo);
        task.setUser(assignee); // Giao cho thành viên

        // 5. Lưu xuống DB
        return taskRepository.save(task);
    }

//    public List<GroupTodo> getGroupByUser(String username){
//        return memberRepository.findGroupsIdByUsername(username);
//    }

    private boolean isLeader(Long groupId, String username){
        return memberRepository.findByGroupsId_IdAndUser_Username(groupId, username)
                .map(m -> m.getRole() == GroupRole.LEADER)
                .orElse(false);
    }
}
