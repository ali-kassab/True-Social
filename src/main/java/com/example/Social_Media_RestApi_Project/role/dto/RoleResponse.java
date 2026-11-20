package com.example.Social_Media_RestApi_Project.role.dto;

import com.example.Social_Media_RestApi_Project.role.entity.Role;

public record RoleResponse(Long id, String name) {

    public static RoleResponse from(Role role) {
        return new RoleResponse(role.getId(), role.getName());
    }
}
