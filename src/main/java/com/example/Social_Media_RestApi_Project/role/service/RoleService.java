package com.example.Social_Media_RestApi_Project.role.service;

import com.example.Social_Media_RestApi_Project.exception.ResourceAlreadyExistsException;
import com.example.Social_Media_RestApi_Project.role.dto.CreateRoleRequest;
import com.example.Social_Media_RestApi_Project.role.dto.RoleResponse;
import com.example.Social_Media_RestApi_Project.role.entity.Role;
import com.example.Social_Media_RestApi_Project.role.repository.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RoleService {

    private final RoleRepository roleRepository;

    public RoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> getRoles() {
        return roleRepository.findAll().stream().map(RoleResponse::from).toList();
    }

    @Transactional
    public RoleResponse createRole(CreateRoleRequest request) {
        if (roleRepository.existsByName(request.name())) {
            throw new ResourceAlreadyExistsException("Role " + request.name() + " already exists");
        }
        return RoleResponse.from(roleRepository.save(new Role(request.name())));
    }
}
