package com.example.inventory.model.dto;

import lombok.Data;
import java.util.List;

@Data
public class FolderContentDTO {
    private Long currentFolderId;
    private String currentFolderName;
    private Long parentFolderId; // Để nút "Back" biết đường quay lại

    private List<FolderItemDTO> subFolders;
    private List<FileItemDTO> files;

    @Data
    public static class FolderItemDTO {
        private Long id;
        private String name;
        private String createdBy;
        private String createdAt;
    }

    @Data
    public static class FileItemDTO {
        private Long id;
        private String name;
        private String url;
        private String type;
        private String uploadedBy;
        private String uploadedAt;
    }
}