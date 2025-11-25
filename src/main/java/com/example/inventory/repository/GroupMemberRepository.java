package com.example.inventory.repository;

import com.example.inventory.model.GroupMember;
import com.example.inventory.model.GroupTodo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

    // Sửa thành GroupsId_Id (Hơi lạ nhưng đúng với tên biến của bạn)
    Optional<GroupMember> findByGroupsId_IdAndUser_Username(Long groupId, String username);

    // Tương tự cho hàm exists
    boolean existsByGroupsId_IdAndUser_Id(Long groupId, Long userId);

//    @Query("SELECT gm.groupId FROM GroupMember gm JOIN gm.user u WHERE u.username = :username")
//    List<Long> findGroupIdsByUsername(@Param("username") String username);
}