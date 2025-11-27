package com.example.inventory.repository;

import com.example.inventory.model.Folder;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FolderRepository extends JpaRepository<Folder, Long> {
    // Lấy các thư mục gốc của nhóm (parent = null)
    List<Folder> findByGroup_IdAndParentIsNullOrderByCreatedAtDesc(Long groupId);

    // Lấy nội dung bên trong 1 thư mục
    List<Folder> findByParent_IdOrderByCreatedAtDesc(Long parentId);
}