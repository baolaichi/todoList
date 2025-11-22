package com.example.inventory.model;

import com.example.inventory.model.entityEnum.GroupRole;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Data
@Table(name = "group")
public class Group {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private String name;
    private String description;
    private GroupRole groupRole;

    @ManyToOne
    @JoinColumn(name = "created_by_user_id")
    private Users createdBy;

    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL)
    private Set<GroupMember> members;


}
