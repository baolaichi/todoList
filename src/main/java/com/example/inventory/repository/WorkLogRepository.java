package com.example.inventory.repository;

import com.example.inventory.model.WorkLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface WorkLogRepository extends JpaRepository<WorkLog, Long> {

    // Lấy log của task cá nhân
    List<WorkLog> findByTask_IdOrderByCreatedAtDesc(Long taskId);

    // Lấy log của task nhóm (MỚI)
    List<WorkLog> findByTaskGroup_IdOrderByCreatedAtDesc(Long taskGroupId);

    // Lấy báo cáo ngày của nhóm (Sửa lại query để trỏ vào taskGroup)
    @Query("SELECT w FROM WorkLog w WHERE w.taskGroup.group.id = :groupId AND w.createdAt BETWEEN :start AND :end ORDER BY w.createdAt DESC")
    List<WorkLog> findDailyReports(
            @Param("groupId") Long groupId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Modifying // Bắt buộc có để báo hiệu đây là lệnh Delete/Update
    @Query("DELETE FROM WorkLog w WHERE w.taskGroup.id = :taskGroupId")
    void deleteByTaskGroup_Id(@Param("taskGroupId") Long taskGroupId);
}