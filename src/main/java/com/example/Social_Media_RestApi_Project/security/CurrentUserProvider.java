package com.example.Social_Media_RestApi_Project.security;

import com.example.Social_Media_RestApi_Project.role.entity.Role;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Gives services access to the currently authenticated user and to ownership checks.
 */
@Component
public class CurrentUserProvider {

    public UserPrincipal getPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return principal;
        }
        throw new AuthenticationCredentialsNotFoundException("No authenticated user");
    }

    public Long getCurrentUserId() {
        return getPrincipal().getId();
    }

    public boolean isAdmin() {
        return getPrincipal().getAuthorities().stream()
                .anyMatch(authority -> Role.ADMIN.equals(authority.getAuthority()));
    }

    /**
     * A resource can be modified by its owner or by an administrator.
     *
     * @param ownerId the id of the user who owns the resource
     */
    public boolean canModify(Long ownerId) {
        return Objects.equals(getCurrentUserId(), ownerId) || isAdmin();
    }
}
