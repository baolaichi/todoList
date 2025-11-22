package com.example.inventory.controller;

import com.example.inventory.model.dto.CreateGroupDTO;
import com.example.inventory.service.GroupService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user/group")
public class GroupController {
    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @PostMapping("/create")
    public ResponseEntity<?> createGroup(@RequestBody CreateGroupDTO groupDTO){
        String username = SecurityContextHolder.getContext(). getAuthentication().getName();
        return  ResponseEntity.ok(groupService.createGroup(groupDTO, username));
    }
}
