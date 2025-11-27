package com.example.inventory.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "file_metadata")
public class FileMetadata {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileName;
    private String fileUrl; // Đường dẫn tải file
    private String fileType; // IMAGE, PDF, DOC...
    private long size;

    private LocalDateTime uploadedAt = LocalDateTime.now();

    @ManyToOne
    @JoinColumn(name = "uploaded_by")
    private Users uploadedBy;

    @ManyToOne
    @JoinColumn(name = "folder_id")
    private Folder folder;
}