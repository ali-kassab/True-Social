package com.example.Social_Media_RestApi_Project.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-key-with-at-least-32-bytes";

    private final UserPrincipal principal = new UserPrincipal(42L, "ali@example.com", "hash",
            Set.<GrantedAuthority>of(new SimpleGrantedAuthority("ROLE_USER")));

    @Test
    void generatedToken_containsUserId() {
        JwtService jwtService = new JwtService(new JwtProperties(SECRET, Duration.ofHours(1)));

        String token = jwtService.generateToken(principal);

        assertThat(jwtService.extractUserId(token)).contains(42L);
        assertThat(jwtService.getExpirationSeconds()).isEqualTo(3600L);
    }

    @Test
    void expiredToken_isRejected() {
        JwtService jwtService = new JwtService(new JwtProperties(SECRET, Duration.ofSeconds(-5)));

        String token = jwtService.generateToken(principal);

        assertThat(jwtService.extractUserId(token)).isEmpty();
    }

    @Test
    void tokenSignedWithAnotherKey_isRejected() {
        JwtService issuer = new JwtService(new JwtProperties("another-secret-key-that-is-also-32-bytes-long", Duration.ofHours(1)));
        JwtService verifier = new JwtService(new JwtProperties(SECRET, Duration.ofHours(1)));

        String token = issuer.generateToken(principal);

        assertThat(verifier.extractUserId(token)).isEmpty();
    }

    @Test
    void malformedToken_isRejected() {
        JwtService jwtService = new JwtService(new JwtProperties(SECRET, Duration.ofHours(1)));

        assertThat(jwtService.extractUserId("not-a-jwt")).isEmpty();
    }

    @Test
    void shortSecret_failsFast() {
        assertThatThrownBy(() -> new JwtService(new JwtProperties("too-short", Duration.ofHours(1))))
                .isInstanceOf(IllegalStateException.class);
    }
}
