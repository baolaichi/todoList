package com.example.inventory.service.impl;

import com.example.inventory.model.Group;
import com.example.inventory.model.Task;
import com.example.inventory.model.Users;
import com.example.inventory.model.dto.CreateGroupDTO;
import com.example.inventory.model.dto.TaskDTO;
import com.example.inventory.repository.GroupMemberRepository;
import com.example.inventory.repository.GroupRepository;
import com.example.inventory.repository.TaskRepository;
import com.example.inventory.repository.UserRepository;
import com.example.inventory.service.GroupService;
import com.example.inventory.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GroupServiceImpl implements GroupService {
    private final GroupRepository groupRepository;
    private final GroupMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;

    public GroupServiceImpl(GroupRepository groupRepository, GroupMemberRepository memberRepository, UserRepository userRepository, TaskRepository taskRepository) {
        this.groupRepository = groupRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
    }

    @Override
    public Group createGroup(CreateGroupDTO groupDTO, String username){
        Users users = userRepository.findByUsername(username).orElseThrow();

        Group group = new Group();
        group.setName(groupDTO.getName());
        group.setDescription(groupDTO.getDescription());
        group.setCreatedBy(users);

        Group saveGroup = groupRepository.save(group);

        return saveGroup;

    }
}
