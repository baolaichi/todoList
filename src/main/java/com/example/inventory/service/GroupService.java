package com.example.inventory.service;

import com.example.inventory.model.Group;
import com.example.inventory.model.dto.CreateGroupDTO;

public interface GroupService {
    public Group createGroup(CreateGroupDTO groupDTO, String username);
}
