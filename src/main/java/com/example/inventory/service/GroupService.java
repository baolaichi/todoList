package com.example.inventory.service;

import com.example.inventory.model.GroupTodo;
import com.example.inventory.model.Task;
import com.example.inventory.model.dto.CreateGroupDTO;
import com.example.inventory.model.dto.TaskGroupDTO;

public interface GroupService {
    public GroupTodo createGroup(CreateGroupDTO groupDTO, String username);
    public void addMember(Long groupId, Long userIdToAdd, String requesterUsername);
    public Task createTaskInGroup(TaskGroupDTO dto, String requesterUsername);

}
