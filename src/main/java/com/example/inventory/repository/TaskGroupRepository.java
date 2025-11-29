package com.example.inventory.repository;

import com.example.inventory.model.TaskGroup;
import com.example.inventory.model.entityEnum.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TaskGroupRepository extends JpaRepository<TaskGroup, Long> {
    // Lấy tất cả task của một nhóm
    List<TaskGroup> findByGroup_IdOrderByCreatedAtDesc(Long groupId);

    // 1. Quét Email: Chưa xong + Quá hạn + Chưa gửi mail
    List<TaskGroup> findByStatusNotAndDeadlineBeforeAndIsRemindFalse(
            Status status,
            LocalDateTime now
    );

    // 2. Quét Alert Web: Người được giao (assignees) + Chưa xong + Quá hạn + Chưa tắt thông báo
    List<TaskGroup> findByAssignees_UsernameAndStatusNotAndDeadlineBeforeAndIsAlertDismissedFalse(
            String username,
            Status status,
            LocalDateTime now
    );
}