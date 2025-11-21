package com.example.inventory.model;

import com.example.inventory.model.entityEnum.Role;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "users")
@Data
public class Users {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    @Size(min = 6, message = "tối thiểu 6 kí tự")
    private String password;
    @Email(message = "nhập đúng định dạng")
    private String email;

    @Enumerated(EnumType.STRING)
    private Role role = Role.VIEWER;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private Set<Task> tasks;

    private LocalDateTime create_at = LocalDateTime.now();
}
