package com.example.inventory.service.impl;

import com.example.inventory.model.*;
import com.example.inventory.model.dto.FolderContentDTO;
import com.example.inventory.model.entityEnum.GroupRole;
import com.example.inventory.repository.*;
import com.example.inventory.service.FolderService;
import jakarta.annotation.PostConstruct; // Import này để chạy hàm init
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

@Slf4j
@Service
public class FolderServiceImpl implements FolderService {

    @Autowired private FolderRepository folderRepository;
    @Autowired private FileMetadataRepository fileRepository;
    @Autowired private GroupRepository groupRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private GroupMemberRepository groupMemberRepository;
    @Autowired private NotificationRepository notificationRepository; // Inject thêm cái này

    private final long LIMIT_5GB = 5L * 1024 * 1024 * 1024;

    // Thư mục gốc để lưu file
    @Value("${app.upload.dir:uploads}")
    private String uploadDir;
    private Path rootLocation;



    // --- QUAN TRỌNG: Tự động tạo thư mục 'uploads' khi server khởi động ---
    @PostConstruct
    public void init() {
        try {
            this.rootLocation = Paths.get(uploadDir);
            Files.createDirectories(rootLocation);
            log.info("✅ Đã khởi tạo thư mục lưu trữ: " + rootLocation.toAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("Không thể khởi tạo thư mục lưu trữ!", e);
        }
    }

    // 1. Tạo thư mục mới
    @Transactional
    @Override
    public void createFolder(Long groupId, Long parentId, String folderName, String username) {
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
            if (file.isEmpty()) {
                throw new RuntimeException("File không được rỗng");
            }

            // --- BƯỚC 1: LƯU FILE VẬT LÝ ---
            String originalName = StringUtils.cleanPath(file.getOriginalFilename());
            // Tạo tên file duy nhất (UUID) để tránh trùng lặp
            String fileName = UUID.randomUUID().toString() + "_" + originalName;

            // Kiểm tra và tạo thư mục lại lần nữa cho chắc chắn
            if (!Files.exists(rootLocation)) {
                Files.createDirectories(rootLocation);
            }

            // Copy file vào ổ cứng
            Files.copy(file.getInputStream(), this.rootLocation.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
            String fileUrl = "/uploads/" + fileName;

            // --- BƯỚC 2: LƯU METADATA VÀO DB ---
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
            log.error("UPLOAD FILE FAILED", e);
            throw new RuntimeException("Lỗi upload file: " + e.getMessage());
        }
    }

    // Hàm kiểm tra và gửi thông báo
    private void checkGroupStorageLimit(Long groupId) {
        Long totalSize = fileRepository.sumSizeByGroupId(groupId);

        if (totalSize >= LIMIT_5GB) {
            // Tìm Trưởng nhóm để báo
            GroupTodo group = groupRepository.findById(groupId).orElseThrow();
            // Giả sử logic tìm Leader:
            Users leader = groupMemberRepository.findByGroupsId_IdAndRole(groupId, GroupRole.LEADER)
                    .map(GroupMember::getUser)
                    .orElse(group.getCreatedBy());

            // Tạo thông báo
            Notification noti = new Notification();
            noti.setUser(leader);
            noti.setMessage("CẢNH BÁO: Nhóm '" + group.getName() + "' đã sử dụng vượt quá 5GB tài liệu. Vui lòng dọn dẹp!");
            noti.setRead(false);
            notificationRepository.save(noti);

            log.warn("⚠️ Nhóm ID " + groupId + " đã vượt quá 5GB!");
        }
    }

    // --- THÊM HÀM XÓA FILE ---
    @Transactional
    @Override
    public void deleteFile(Long groupId, Long fileId, String username) {
        FileMetadata file = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File không tồn tại"));

        // Check quyền: Phải là Leader HOẶC là người đã upload file đó
        boolean isLeader = isLeader(groupId, username);
        boolean isOwner = file.getUploadedBy().getUsername().equals(username);

        if (!isLeader && !isOwner) {
            throw new RuntimeException("Bạn không có quyền xóa file này!");
        }

        try {
            // 1. Xóa file vật lý trên ổ cứng
            String fileName = file.getFileUrl().replace("/uploads/", "");
            Path filePath = this.rootLocation.resolve(fileName);
            Files.deleteIfExists(filePath);

            // 2. Xóa thông tin trong Database
            fileRepository.delete(file);

        } catch (IOException e) {
            throw new RuntimeException("Lỗi khi xóa file trên ổ cứng", e);
        }
    }

    // --- HÀM XÓA THƯ MỤC ---
    @Transactional
    @Override
    public void deleteFolder(Long groupId, Long folderId, String username) {
        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new RuntimeException("Thư mục không tồn tại"));

        // Check quyền: Chỉ Leader hoặc người tạo thư mục
        boolean isLeader = isLeader(groupId, username);
        boolean isOwner = folder.getCreatedBy().getUsername().equals(username);

        if (!isLeader && !isOwner) {
            throw new RuntimeException("Bạn không có quyền xóa thư mục này!");
        }

        // Logic xóa thư mục:
        // Nếu thư mục có chứa file con -> Cần xóa hết file con vật lý trước (Đệ quy)
        // Ở đây làm đơn giản: Hibernate Cascade sẽ xóa DB, nhưng file vật lý cần xử lý kỹ hơn.
        // Tạm thời chỉ xóa DB, file vật lý sẽ thành file rác (sẽ được dọn bởi StorageCleanupService sau 15 ngày)

        folderRepository.delete(folder);
    }

    // Helper check Leader
    private boolean isLeader(Long groupId, String username) {
        return groupMemberRepository.findByGroupsId_IdAndUser_Username(groupId, username)
                .map(m -> m.getRole() == GroupRole.LEADER)
                .orElse(false);
    }
}