package com.example.inventory.repository;

import com.example.inventory.model.GroupTodo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GroupRepository extends JpaRepository<GroupTodo, Long> {

}
