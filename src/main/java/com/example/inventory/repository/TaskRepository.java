package com.example.inventory.repository;

import com.example.inventory.model.Task;
import com.example.inventory.model.entityEnum.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByUser_Id(Long userId);

    List<Task> findByUser_IdOrderByCreatAtDesc(Long userId);

    List<Task> findByUser_UsernameAndDeadlineBetweenOrderByDeadlineAsc(
            String username,
            LocalDateTime start,
            LocalDateTime end
    );

    List<Task> findByUser_UsernameAndDeadlineAfterOrderByDeadlineAsc(
            String username,
            LocalDateTime now
    );

    List<Task> findByUser_UsernameAndStatusNotAndDeadlineBefore(
            String username,
            Status status,
            LocalDateTime now
    );


    // --- 3. CÁC HÀM CHO HỆ THỐNG NHẮC NHỞ (REMINDER) ---

    List<Task> findByStatusNotAndDeadlineBeforeAndIsRemindFalse(
            Status status,
            LocalDateTime now
    );

    List<Task> findByUser_UsernameAndStatusNotAndDeadlineBeforeAndIsAlertDismissedFalse(
            String username,
            Status status,
            LocalDateTime now
    );

    // Xóa liên kết trong bảng trung gian task_assignees (Nguyên nhân gây lỗi 1451)
    @Modifying
    @Query(value = "DELETE FROM task_assignees WHERE task_id = :taskId", nativeQuery = true)
    void deleteAssigneesByTaskId(@Param("taskId") Long taskId);

    // Xóa các báo cáo tiến độ liên quan đến task này (nếu có)
    @Modifying
    @Query(value = "DELETE FROM work_logs WHERE task_id = :taskId", nativeQuery = true)
    void deleteWorkLogsByTaskId(@Param("taskId") Long taskId);
}