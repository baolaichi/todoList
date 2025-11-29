package com.example.inventory.controller;

import com.example.inventory.model.dto.*;
import com.example.inventory.service.GroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user/groups") // Đổi thành số nhiều 'groups' cho chuẩn
public class GroupController {

    @Autowired
    private GroupService groupService;

    // 1. Lấy danh sách nhóm của tôi
    @GetMapping
    public ResponseEntity<?> getMyGroups() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(groupService.getMyGroups(username));
    }

    // 2. Tạo nhóm mới
    @PostMapping
    public ResponseEntity<?> createGroup(@RequestBody CreateGroupDTO dto) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(groupService.createGroup(dto, username));
    }

    // 3. Lấy chi tiết nhóm
    @GetMapping("/{id}")
    public ResponseEntity<?> getGroupDetail(@PathVariable Long id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(groupService.getGroupDetail(id, username));
    }

    // 4. Thêm thành viên
    @PostMapping("/{id}/members")
    public ResponseEntity<?> addMember(@PathVariable Long id, @RequestBody AddMemberRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            groupService.addMember(id, request, username);
            return ResponseEntity.ok("Thêm thành viên thành công");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 5. Lấy danh sách thành viên
    @GetMapping("/{id}/members")
    public ResponseEntity<?> getMembers(@PathVariable Long id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(groupService.getGroupMembers(id, username));
    }

    // --- TASK TRONG NHÓM ---

    // 6. Lấy danh sách task
    @GetMapping("/{id}/tasks")
    public ResponseEntity<?> getGroupTasks(@PathVariable Long id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(groupService.getTasksByGroupId(id, username));
    }

    // 7. Tạo task nhóm
    @PostMapping("/tasks")
    public ResponseEntity<?> createTaskInGroup(@RequestBody TaskGroupDTO dto) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            return ResponseEntity.ok(groupService.createTaskInGroup(dto, username));
        } catch (Exception e) {
            // IN LỖI RA CONSOLE ĐỂ DEBUG
            e.printStackTrace();
            String errorMsg = e.getMessage() != null ? e.getMessage() : "Lỗi hệ thống (NullPointer): " + e.getClass().getName();
            return ResponseEntity.badRequest().body(errorMsg);
        }
    }

    // 8. Cập nhật task nhóm
    @PutMapping("/tasks/{taskId}")
    public ResponseEntity<?> updateTaskInGroup(@PathVariable Long taskId, @RequestBody TaskGroupDTO dto) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            return ResponseEntity.ok(groupService.updateTaskInGroup(taskId, dto, username));
        } catch (Exception e) {
            // IN LỖI RA CONSOLE ĐỂ DEBUG
            e.printStackTrace();
            String errorMsg = e.getMessage() != null ? e.getMessage() : "Lỗi hệ thống (NullPointer): " + e.getClass().getName();
            return ResponseEntity.badRequest().body(errorMsg);
        }
    }

    @DeleteMapping("/tasks/{taskId}")
    public ResponseEntity<?> deleteTaskInGroup(@PathVariable Long taskId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            // Bạn nhớ thêm hàm deleteTaskInGroup vào interface GroupService trước nhé
            groupService.deleteTaskInGroup(taskId, username);
            return ResponseEntity.ok("Đã xóa công việc thành công");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/tasks/{taskId}/report")
    public ResponseEntity<?> createWorkLog(@PathVariable Long taskId, @RequestBody Map<String, String> body) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        String content = body.get("content");
        try {
            groupService.createWorkLog(taskId, content, username);
            return ResponseEntity.ok("Đã gửi báo cáo");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // API lấy lịch sử báo cáo (Giữ nguyên URL này vì Frontend đang gọi đúng)
    @GetMapping("/tasks/{taskId}/work-logs")
    public ResponseEntity<?> getWorkLogs(@PathVariable Long taskId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        // Thêm username vào hàm getWorkLogs để kiểm tra quyền xem
        return ResponseEntity.ok(groupService.getWorkLogs(taskId, username));
    }

    @GetMapping("/{groupId}/daily-reports")
    public ResponseEntity<?> getDailyReports(@PathVariable Long groupId) {
        return ResponseEntity.ok(groupService.getDailyReports(groupId));
    }

    // API Tắt thông báo cho Task Nhóm (Dùng cho nút Đã hiểu bên Frontend)
    @PatchMapping("/tasks/{taskId}/dismiss")
    public ResponseEntity<?> dismissGroupAlert(@PathVariable Long taskId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        // Bạn cần inject ReminderService và gọi hàm dismissGroupTaskAlert(taskId, username) ở đây
        // Tạm thời trả về OK để ko lỗi 404 nếu chưa implement service
        return ResponseEntity.ok("Đã tắt thông báo");
    }

    @GetMapping("/tasks/{taskId}")
    public ResponseEntity<?> getTaskGroupDetail(@PathVariable Long taskId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        // Gọi hàm getTaskGroupDetail mà bạn đã viết trong Service
        return ResponseEntity.ok(groupService.getTaskGroupDetail(taskId, username));
    }
}