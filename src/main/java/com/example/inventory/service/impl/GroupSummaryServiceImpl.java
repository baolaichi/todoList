package com.example.inventory.service.impl;

import com.example.inventory.model.map.GroupSummary;
import com.example.inventory.repository.GroupSummaryRepository;
import com.example.inventory.service.GroupSummaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GroupSummaryServiceImpl implements GroupSummaryService {
    @Autowired
    private GroupSummaryRepository summaryRepository;

    @Override
    public GroupSummary getGroupDetail(Long groupId) {
        return summaryRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Group với ID này")); // Sửa message lỗi cho đúng ngữ cảnh
    }
}
