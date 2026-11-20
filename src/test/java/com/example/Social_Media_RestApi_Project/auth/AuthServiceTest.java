package com.example.Social_Media_RestApi_Project.auth;

import com.example.Social_Media_RestApi_Project.auth.dto.LoginResponse;
import com.example.Social_Media_RestApi_Project.auth.dto.RegisterRequest;
import com.example.Social_Media_RestApi_Project.auth.service.AuthService;
import com.example.Social_Media_RestApi_Project.exception.ResourceAlreadyExistsException;
import com.example.Social_Media_RestApi_Project.role.entity.Role;
import com.example.Social_Media_RestApi_Project.role.repository.RoleRepository;
import com.example.Social_Media_RestApi_Project.security.JwtService;
import com.example.Social_Media_RestApi_Project.security.UserPrincipal;
import com.example.Social_Media_RestApi_Project.user.entity.User;
import com.example.Social_Media_RestApi_Project.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtService jwtService;

    @InjectMocks private AuthService authService;

    @Test
    void register_hashesPasswordAssignsUserRoleAndReturnsToken() {
        Role userRole = new Role(Role.USER);
        when(userRepository.existsByEmail("ali@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("bcrypt-hash");
        when(roleRepository.findByName(Role.USER)).thenReturn(Optional.of(userRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            ReflectionTestUtils.setField(user, "id", 1L);
            return user;
        });
        when(jwtService.generateToken(any(UserPrincipal.class))).thenReturn("jwt-token");
        when(jwtService.getExpirationSeconds()).thenReturn(3600L);

        LoginResponse response = authService.register(new RegisterRequest("Ali", " Ali@Example.com ", "password123"));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("ali@example.com");
        assertThat(saved.getValue().getPassword()).isEqualTo("bcrypt-hash");
        assertThat(saved.getValue().getRoles()).containsExactly(userRole);
        assertThat(response.accessToken()).isEqualTo("jwt-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.user().id()).isEqualTo(1L);
    }

    @Test
    void register_withExistingEmail_isConflict() {
        when(userRepository.existsByEmail("ali@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterRequest("Ali", "ali@example.com", "password123")))
                .isInstanceOf(ResourceAlreadyExistsException.class);
        verify(userRepository, never()).save(any());
    }
}
