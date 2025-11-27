package com.example.inventory.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@Table(name = "folders")
public class Folder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne
    @JoinColumn(name = "group_id")
    @JsonIgnore
    private GroupTodo group;

    @ManyToOne
    @JoinColumn(name = "created_by")
    private Users createdBy;

    // Thư mục cha (Nếu null thì là thư mục gốc của nhóm)
    @ManyToOne
    @JoinColumn(name = "parent_id")
    @JsonIgnore
    private Folder parent;

    // Danh sách thư mục con
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
    private List<Folder> subFolders;

    // Danh sách file trong thư mục này
    @OneToMany(mappedBy = "folder", cascade = CascadeType.ALL)
    private List<FileMetadata> files;
}