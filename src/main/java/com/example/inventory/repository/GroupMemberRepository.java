package com.example.inventory.repository;

import com.example.inventory.model.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

    // Sửa thành GroupsId_Id (Hơi lạ nhưng đúng với tên biến của bạn)
    Optional<GroupMember> findByGroupsId_IdAndUser_Username(Long groupId, String username);

    // Tương tự cho hàm exists
    boolean existsByGroupsId_IdAndUser_Id(Long groupId, Long userId);
}