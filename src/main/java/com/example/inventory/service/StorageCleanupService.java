package com.example.inventory.service;

import com.example.inventory.model.FileMetadata;
import com.example.inventory.model.GroupTodo;
import com.example.inventory.repository.FileMetadataRepository;
import com.example.inventory.repository.GroupRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value; // Import này
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class StorageCleanupService {

    @Autowired private GroupRepository groupRepository;
    @Autowired private FileMetadataRepository fileRepository;

    // Lấy đường dẫn từ application.properties
    // Nếu không có thì mặc định lấy thư mục 'uploads' trong project
    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    // Chạy vào 2:00 sáng mỗi ngày
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void autoCleanupGroups() {
        System.out.println("--- BẮT ĐẦU QUÉT DỌN DẸP TÀI LIỆU NHÓM (15 NGÀY) ---");

        List<GroupTodo> allGroups = groupRepository.findAll();
        LocalDateTime now = LocalDateTime.now();

        for (GroupTodo group : allGroups) {
            // Kiểm tra xem đã quá 15 ngày kể từ lần dọn cuối cùng chưa
            long daysDiff = ChronoUnit.DAYS.between(group.getLastCleanupDate(), now);

            if (daysDiff >= 15) {
                cleanGroupStorage(group);

                // Cập nhật lại ngày dọn dẹp là hôm nay
                group.setLastCleanupDate(now);
                groupRepository.save(group);
                System.out.println("✅ Đã dọn dẹp nhóm: " + group.getName());
            }
        }
    }

    private void cleanGroupStorage(GroupTodo group) {
        // 1. Lấy tất cả file của nhóm này
        List<FileMetadata> files = fileRepository.findByFolder_Group_Id(group.getId());

        // 2. Xóa từng file
        for (FileMetadata fileMeta : files) {
            try {
                // Xóa file vật lý trên ổ cứng
                String fileName = fileMeta.getFileUrl().replace("/uploads/", "");

                // SỬA Ở ĐÂY: Dùng biến uploadDir thay vì đường dẫn cứng
                Path rootLocation = Paths.get(uploadDir);
                Path filePath = rootLocation.resolve(fileName);

                Files.deleteIfExists(filePath);

                // Xóa metadata trong DB
                fileRepository.delete(fileMeta);

            } catch (IOException e) {
                System.err.println("Lỗi xóa file: " + fileMeta.getFileName());
            }
        }
    }
}