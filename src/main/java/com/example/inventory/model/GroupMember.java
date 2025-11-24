package com.example.inventory.model;

import com.example.inventory.model.entityEnum.GroupRole;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "group_member")
public class GroupMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne
    @JoinColumn(name = "group_id")
    private GroupTodo groupsId;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private Users user;

    @Enumerated(EnumType.STRING)
    private GroupRole role; // LEADER hoặc MEMBER
}
