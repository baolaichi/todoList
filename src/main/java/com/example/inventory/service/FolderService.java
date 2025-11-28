package com.example.inventory.service;

import com.example.inventory.model.dto.FolderContentDTO;
import org.springframework.web.multipart.MultipartFile;

public interface FolderService {
    public void createFolder(Long groupId, Long parentId, String folderName, String username);
    public FolderContentDTO getFolderContent(Long groupId, Long folderId, String username);
    public void uploadFile(Long groupId, Long folderId, MultipartFile file, String username);
    public void deleteFolder(Long groupId, Long folderId, String username);
    public void deleteFile(Long groupId, Long fileId, String username);
}
