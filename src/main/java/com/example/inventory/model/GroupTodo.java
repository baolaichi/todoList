package com.example.inventory.model;

import com.example.inventory.model.entityEnum.GroupRole;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Data
@Table(name = "groups_todo")
public class GroupTodo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private String name;
    private String description;
    @Enumerated(EnumType.STRING)
    private GroupRole groupRole;

    @ManyToOne
    @JoinColumn(name = "created_by_user_id")
    private Users createdBy;

    private LocalDateTime createdAt = LocalDateTime.now();

    private int memberCount;


}
