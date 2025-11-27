package com.example.inventory.service;

import com.example.inventory.model.GroupTodo;
import com.example.inventory.model.Task;
import com.example.inventory.model.dto.*;

import java.util.List;

public interface GroupService {
    public GroupDTO createGroup(CreateGroupDTO groupDTO, String username);
    public void addMember(Long groupId, AddMemberRequest request, String requesterUsername);
    public Task createTaskInGroup(TaskGroupDTO dto, String requesterUsername);
    public List<GroupDTO> getMyGroups(String username);
    public GroupDTO getGroupDetail(Long groupId, String username);
    public List<GroupMemberDTO> getGroupMembers(Long groupId, String username);
    public Task updateTaskInGroup(Long taskId, TaskDTO dto, String requesterUsername);
    public List<TaskDTO> getTasksByGroupId(Long groupId, String username);
    public void deleteTaskInGroup(Long taskId, String username);

}
