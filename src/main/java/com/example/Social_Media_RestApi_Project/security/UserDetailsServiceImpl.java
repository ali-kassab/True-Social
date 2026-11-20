package com.example.Social_Media_RestApi_Project.security;

import com.example.Social_Media_RestApi_Project.user.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** Used by the login flow, where the "username" is the email address. */
    @Override
    public UserPrincipal loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .map(UserPrincipal::from)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    /** Used by the JWT filter, where the token subject is the user id. */
    public UserPrincipal loadUserById(Long id) throws UsernameNotFoundException {
        return userRepository.findById(id)
                .map(UserPrincipal::from)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}
