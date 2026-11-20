package com.example.Social_Media_RestApi_Project.role;

import com.example.Social_Media_RestApi_Project.role.entity.Role;
import com.example.Social_Media_RestApi_Project.role.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Makes sure the built-in roles exist when the application starts.
 */
@Component
public class RoleDataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;

    public RoleDataInitializer(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public void run(String... args) {
        createRoleIfMissing(Role.USER);
        createRoleIfMissing(Role.ADMIN);
    }

    private void createRoleIfMissing(String roleName) {
        if (!roleRepository.existsByName(roleName)) {
            roleRepository.save(new Role(roleName));
        }
    }
}
