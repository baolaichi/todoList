package com.example.inventory.repository;

import com.example.inventory.model.Task;
import com.example.inventory.model.dto.ShowTask;
import com.example.inventory.model.entityEnum.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    public Task existsById(long id);
    public List<Task> findByUser_Id(Long userId);
    List<Task> findByUser_UsernameAndDeadlineBetweenOrderByDeadlineAsc(
            String username,
            LocalDateTime start,
            LocalDateTime end
    );

    List<Task> findByUser_UsernameAndDeadlineBefore(String username, LocalDateTime date);

    // Dùng cho UPCOMING
    List<Task> findByUser_UsernameAndDeadlineAfterOrderByDeadlineAsc(String username, LocalDateTime now);

    // Dùng cho OVERDUE (Cần thêm điều kiện Status.DONE)
    List<Task> findByUser_UsernameAndStatusNotAndDeadlineBefore(String username, Status status, LocalDateTime now);
}
