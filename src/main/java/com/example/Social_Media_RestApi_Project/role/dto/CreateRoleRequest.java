package com.example.Social_Media_RestApi_Project.role.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateRoleRequest(
        @NotBlank(message = "Role name is required")
        @Pattern(regexp = "^ROLE_[A-Z_]{2,45}$", message = "Role name must look like ROLE_NAME (upper case)")
        String name
) {
}
