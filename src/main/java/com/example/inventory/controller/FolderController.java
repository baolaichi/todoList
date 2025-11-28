package com.example.inventory.controller;

import com.example.inventory.service.impl.FolderServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

@RestController
@RequestMapping("/api/user/groups")
public class FolderController {

    @Autowired
    private FolderServiceImpl folderService;

    @GetMapping("/{groupId}/folders")
    public ResponseEntity<?> getFolderContent(
            @PathVariable Long groupId,
            @RequestParam(required = false) Long folderId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(folderService.getFolderContent(groupId, folderId, username));
    }

    @PostMapping("/{groupId}/folders")
    public ResponseEntity<?> createFolder(
            @PathVariable Long groupId,
            @RequestBody Map<String, Object> payload) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        String name = (String) payload.get("name");
        Long parentId = payload.get("parentId") != null ? Long.valueOf(payload.get("parentId").toString()) : null;

        folderService.createFolder(groupId, parentId, name, username);
        return ResponseEntity.ok("Tạo thư mục thành công");
    }

    @PostMapping(value = "/{groupId}/folders/{folderId}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadFile(
            @PathVariable Long groupId,
            @PathVariable Long folderId,
            @RequestParam("file") MultipartFile file) {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        folderService.uploadFile(groupId, folderId, file, username);
        return ResponseEntity.ok("Upload file thành công");
    }

    @DeleteMapping("/{groupId}/files/{fileId}")
    public ResponseEntity<?> deleteFile(
            @PathVariable Long groupId,
            @PathVariable Long fileId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        folderService.deleteFile(groupId, fileId, username);
        return ResponseEntity.ok("Đã xóa file thành công");
    }

    @DeleteMapping("/{groupId}/folders/{folderId}")
    public ResponseEntity<?> deleteFolder(
            @PathVariable Long groupId,
            @PathVariable Long folderId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        folderService.deleteFolder(groupId, folderId, username);
        return ResponseEntity.ok("Đã xóa thư mục thành công");
    }
}