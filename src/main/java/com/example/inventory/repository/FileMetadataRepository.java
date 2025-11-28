package com.example.inventory.repository;

import com.example.inventory.model.FileMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface FileMetadataRepository extends JpaRepository<FileMetadata, Long> {
    // Tính tổng size (bytes) của tất cả file trong nhóm
    // Dùng COALESCE để trả về 0 nếu chưa có file nào (tránh null)
    @Query("SELECT COALESCE(SUM(f.size), 0) FROM FileMetadata f WHERE f.folder.group.id = :groupId")
    Long sumSizeByGroupId(Long groupId);

    // Tìm tất cả file của một nhóm (để xóa)
    List<FileMetadata> findByFolder_Group_Id(Long groupId);
}