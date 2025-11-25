package com.example.inventory.controller;

import com.example.inventory.model.dto.CreateGroupDTO;
import com.example.inventory.model.dto.TaskGroupDTO;
import com.example.inventory.model.map.GroupSummary;
import com.example.inventory.service.GroupService;
import com.example.inventory.service.GroupSummaryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user/group")
public class GroupController {
    private final GroupService groupService;
    private final GroupSummaryService summaryService;

    public GroupController(GroupService groupService, GroupSummaryService summaryService) {
        this.groupService = groupService;
        this.summaryService = summaryService;
    }

    @PostMapping("/create")
    public ResponseEntity<?> createGroup(@RequestBody CreateGroupDTO groupDTO){
        String username = SecurityContextHolder.getContext(). getAuthentication().getName();
        return  ResponseEntity.ok(groupService.createGroup(groupDTO, username));
    }


    @PostMapping("/{groupId}/add_member")
    public ResponseEntity<?> addMember(@PathVariable Long groupId, @RequestBody Long userId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        groupService.addMember(groupId, userId,username);
        return ResponseEntity.ok("Thêm thành viên thành công");
    }

    @GetMapping("/{groupId}/member")
    public ResponseEntity<?> getGroupDashboard(@PathVariable Long groupId) {
        return ResponseEntity.ok(summaryService.getGroupDetail(groupId));
    }

    @PostMapping("/task_group")
    public ResponseEntity<?> createTaskInGroup(@RequestBody TaskGroupDTO dto){
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(groupService.createTaskInGroup(dto, username));
    }
}
