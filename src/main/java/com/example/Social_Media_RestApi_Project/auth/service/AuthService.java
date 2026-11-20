package com.example.Social_Media_RestApi_Project.auth.service;

import com.example.Social_Media_RestApi_Project.auth.dto.LoginRequest;
import com.example.Social_Media_RestApi_Project.auth.dto.LoginResponse;
import com.example.Social_Media_RestApi_Project.auth.dto.RegisterRequest;
import com.example.Social_Media_RestApi_Project.exception.ResourceAlreadyExistsException;
import com.example.Social_Media_RestApi_Project.role.entity.Role;
import com.example.Social_Media_RestApi_Project.role.repository.RoleRepository;
import com.example.Social_Media_RestApi_Project.security.JwtService;
import com.example.Social_Media_RestApi_Project.security.UserPrincipal;
import com.example.Social_Media_RestApi_Project.user.dto.UserSummary;
import com.example.Social_Media_RestApi_Project.user.entity.User;
import com.example.Social_Media_RestApi_Project.user.repository.UserRepository;
import com.example.Social_Media_RestApi_Project.user.service.UserService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    /** Creates a new account with ROLE_USER and returns an access token for it. */
    @Transactional
    public LoginResponse register(RegisterRequest request) {
        String email = UserService.normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new ResourceAlreadyExistsException("Email is already registered");
        }

        User user = new User(request.name().trim(), email, passwordEncoder.encode(request.password()));
        Role userRole = roleRepository.findByName(Role.USER)
                .orElseGet(() -> roleRepository.save(new Role(Role.USER)));
        user.addRole(userRole);
        User saved = userRepository.save(user);

        return tokenFor(UserPrincipal.from(saved), UserSummary.from(saved));
    }

    /**
     * Verifies the credentials with Spring Security's AuthenticationManager (BCrypt comparison)
     * and issues a JWT. Wrong credentials raise BadCredentialsException (HTTP 401).
     */
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(UserService.normalizeEmail(request.email()), request.password()));
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.getReferenceById(principal.getId());
        return tokenFor(principal, UserSummary.from(user));
    }

    private LoginResponse tokenFor(UserPrincipal principal, UserSummary user) {
        return LoginResponse.bearer(jwtService.generateToken(principal), jwtService.getExpirationSeconds(), user);
    }
}
