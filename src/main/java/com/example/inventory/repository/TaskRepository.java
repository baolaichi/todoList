package com.example.inventory.repository;

import com.example.inventory.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    public Task existsById(long id);
    public List<Task> findByUser_Id(Long userId);
}
