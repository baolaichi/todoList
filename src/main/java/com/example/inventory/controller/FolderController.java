package com.example.inventory.controller;

import com.example.inventory.service.impl.FolderServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
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

    // 1. Lấy nội dung thư mục
    // GET /api/user/groups/{groupId}/folders?folderId=...
    @GetMapping("/{groupId}/folders")
    public ResponseEntity<?> getFolderContent(
            @PathVariable Long groupId,
            @RequestParam(required = false) Long folderId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(folderService.getFolderContent(groupId, folderId, username));
    }

    // 2. Tạo thư mục mới
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

    // 3. Upload file vào thư mục
    @PostMapping("/{groupId}/folders/{folderId}/files")
    public ResponseEntity<?> uploadFile(
            @PathVariable Long groupId,
            @PathVariable Long folderId,
            @RequestParam("file") MultipartFile file) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        folderService.uploadFile(groupId, folderId, file, username);
        return ResponseEntity.ok("Upload file thành công");
    }
}