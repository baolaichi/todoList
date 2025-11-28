package com.example.inventory.repository;

import aj.org.objectweb.asm.commons.Remapper;
import com.example.inventory.model.GroupMember;
import com.example.inventory.model.GroupTodo;
import com.example.inventory.model.entityEnum.GroupRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

    Optional<GroupMember> findByGroupsId_IdAndUser_Username(Long groupId, String username);

    // 2. Check xem user đã trong nhóm chưa
    boolean existsByGroupsId_IdAndUser_Id(Long groupId, Long userId);
    boolean existsByGroupsId_IdAndUser_Username(Long groupId, String username); // Check bằng username cho tiện

    // 3. Lấy danh sách các nhóm mà user này tham gia (Để hiển thị trang "Nhóm của tôi")
    List<GroupMember> findByUser_Username(String username);

    // 4. Lấy danh sách thành viên của 1 nhóm (Để hiển thị trong Group Detail)
    List<GroupMember> findByGroupsId_Id(Long groupId);

    Optional<GroupMember> findByGroupsId_IdAndRole(Long groupId, GroupRole role);
}