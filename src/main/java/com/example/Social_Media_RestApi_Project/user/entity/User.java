package com.example.Social_Media_RestApi_Project.user.entity;

import com.example.Social_Media_RestApi_Project.common.entity.BaseEntity;
import com.example.Social_Media_RestApi_Project.role.entity.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    /** BCrypt hash - the raw password is never stored. */
    @Column(nullable = false)
    private String password;

    /** Public HTTPS URL of the profile photo. */
    @Column(name = "profile_image")
    private String profileImage;

    @Column(length = 160)
    private String bio;

    // Roles are few and always needed to build the security principal, so EAGER is acceptable here
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    public User(String name, String email, String password) {
        this.name = name;
        this.email = email;
        this.password = password;
    }

    public void addRole(Role role) {
        roles.add(role);
    }
}
