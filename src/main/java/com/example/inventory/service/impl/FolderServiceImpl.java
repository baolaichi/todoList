package com.example.inventory.service.impl;

import com.example.inventory.model.*;
import com.example.inventory.model.dto.FolderContentDTO;
import com.example.inventory.repository.*;
import com.example.inventory.service.FolderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FolderServiceImpl implements FolderService {

    @Autowired private FolderRepository folderRepository;
    @Autowired private FileMetadataRepository fileRepository;
    @Autowired private GroupRepository groupRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private GroupMemberRepository groupMemberRepository;

    private final Path rootLocation = Paths.get("uploads");

    // 1. Tạo thư mục mới
    @Transactional
    @Override
    public void createFolder(Long groupId, Long parentId, String folderName, String username) {
        // Check quyền thành viên
        if (!groupMemberRepository.existsByGroupsId_IdAndUser_Username(groupId, username)) {
            throw new RuntimeException("Bạn không phải thành viên nhóm này");
        }

        Users creator = userRepository.findByUsername(username).orElseThrow();
        GroupTodo group = groupRepository.findById(groupId).orElseThrow();

        Folder folder = new Folder();
        folder.setName(folderName);
        folder.setGroup(group);
        folder.setCreatedBy(creator);

        if (parentId != null) {
            Folder parent = folderRepository.findById(parentId).orElseThrow();
            folder.setParent(parent);
        }

        folderRepository.save(folder);
    }

    // 2. Lấy nội dung thư mục (Folder + Files)
    @Override
    public FolderContentDTO getFolderContent(Long groupId, Long folderId, String username) {
        if (!groupMemberRepository.existsByGroupsId_IdAndUser_Username(groupId, username)) {
            throw new RuntimeException("Bạn không phải thành viên nhóm này");
        }

        FolderContentDTO dto = new FolderContentDTO();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        if (folderId == null) {
            // Lấy thư mục gốc của nhóm
            dto.setCurrentFolderId(null);
            dto.setCurrentFolderName("Thư mục gốc");
            dto.setParentFolderId(null);

            dto.setSubFolders(folderRepository.findByGroup_IdAndParentIsNullOrderByCreatedAtDesc(groupId).stream().map(f -> {
                FolderContentDTO.FolderItemDTO item = new FolderContentDTO.FolderItemDTO();
                item.setId(f.getId());
                item.setName(f.getName());
                item.setCreatedBy(f.getCreatedBy().getUsername());
                item.setCreatedAt(f.getCreatedAt().format(formatter));
                return item;
            }).collect(Collectors.toList()));

            // Thư mục gốc tạm thời không cho để file lẻ (để gọn), hoặc bạn có thể query thêm
            dto.setFiles(List.of());
        } else {
            // Lấy nội dung thư mục con
            Folder current = folderRepository.findById(folderId).orElseThrow();
            dto.setCurrentFolderId(current.getId());
            dto.setCurrentFolderName(current.getName());
            dto.setParentFolderId(current.getParent() != null ? current.getParent().getId() : null);

            dto.setSubFolders(current.getSubFolders().stream().map(f -> {
                FolderContentDTO.FolderItemDTO item = new FolderContentDTO.FolderItemDTO();
                item.setId(f.getId());
                item.setName(f.getName());
                item.setCreatedBy(f.getCreatedBy().getUsername());
                item.setCreatedAt(f.getCreatedAt().format(formatter));
                return item;
            }).collect(Collectors.toList()));

            dto.setFiles(current.getFiles().stream().map(f -> {
                FolderContentDTO.FileItemDTO item = new FolderContentDTO.FileItemDTO();
                item.setId(f.getId());
                item.setName(f.getFileName());
                item.setUrl(f.getFileUrl());
                item.setType(f.getFileType());
                item.setUploadedBy(f.getUploadedBy().getUsername());
                item.setUploadedAt(f.getUploadedAt().format(formatter));
                return item;
            }).collect(Collectors.toList()));
        }
        return dto;
    }

    // 3. Upload file vào thư mục
    @Transactional
    @Override
    public void uploadFile(Long groupId, Long folderId, MultipartFile file, String username) {
        if (folderId == null) throw new RuntimeException("Phải chọn thư mục để upload!");

        try {
            // Lưu file vật lý
            String originalName = StringUtils.cleanPath(file.getOriginalFilename());
            String fileName = UUID.randomUUID().toString() + "_" + originalName;
            Files.copy(file.getInputStream(), this.rootLocation.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
            String fileUrl = "/uploads/" + fileName;

            // Lưu Metadata vào DB
            Users uploader = userRepository.findByUsername(username).orElseThrow();
            Folder folder = folderRepository.findById(folderId).orElseThrow();

            FileMetadata meta = new FileMetadata();
            meta.setFileName(originalName);
            meta.setFileUrl(fileUrl);
            meta.setFileType(file.getContentType());
            meta.setSize(file.getSize());
            meta.setUploadedBy(uploader);
            meta.setFolder(folder);

            fileRepository.save(meta);

        } catch (IOException e) {
            throw new RuntimeException("Lỗi upload file");
        }
    }
}