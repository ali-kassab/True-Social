package com.example.Social_Media_RestApi_Project.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Reads the {@code Authorization: Bearer <token>} header and, when the token is valid,
 * authenticates the request. Invalid tokens are not rejected here: the request simply stays
 * anonymous and Spring Security returns 401 for protected endpoints.
 * <p>
 * Not a Spring bean on purpose: it is registered only inside the security filter chain.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** Request attribute read by {@link RestAuthenticationEntryPoint} to explain a 401. */
    public static final String AUTH_ERROR_ATTRIBUTE = "auth_error";

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsServiceImpl userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header != null && header.startsWith(BEARER_PREFIX)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            authenticate(header.substring(BEARER_PREFIX.length()), request);
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(String token, HttpServletRequest request) {
        Optional<Long> userId = jwtService.extractUserId(token);
        if (userId.isEmpty()) {
            request.setAttribute(AUTH_ERROR_ATTRIBUTE, "Invalid or expired token");
            return;
        }
        try {
            UserPrincipal principal = userDetailsService.loadUserById(userId.get());
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (UsernameNotFoundException ex) {
            request.setAttribute(AUTH_ERROR_ATTRIBUTE, "The user of this token no longer exists");
        }
    }
}
