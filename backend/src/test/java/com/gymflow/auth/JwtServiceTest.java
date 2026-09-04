package com.gymflow.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gymflow.auth.domain.Role;
import com.gymflow.auth.domain.User;
import com.gymflow.auth.infrastructure.security.AuthenticatedPrincipal;
import com.gymflow.auth.infrastructure.security.JwtProperties;
import com.gymflow.auth.infrastructure.security.JwtService;
import io.jsonwebtoken.JwtException;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Unit puro, sin contexto de Spring — el dominio de tokens no debería necesitarlo. */
class JwtServiceTest {

    private static JwtProperties properties(Duration accessTtl) {
        JwtProperties props = new JwtProperties();
        props.setSecret("unit-test-secret-key-at-least-32-bytes-long");
        props.setAccessTokenTtl(accessTtl);
        props.setRefreshTokenTtl(Duration.ofDays(7));
        return props;
    }

    @Test
    void issuedTokenParsesBackToTheSameClaims() {
        JwtService jwtService = new JwtService(properties(Duration.ofMinutes(15)));
        UUID gymId = UUID.randomUUID();
        User user = new User(UUID.randomUUID(), "trainer@gymflow.dev", "irrelevant-hash", Role.TRAINER, gymId, true, java.time.Instant.now());

        String token = jwtService.issueAccessToken(user);
        AuthenticatedPrincipal principal = jwtService.parseAccessToken(token);

        assertThat(principal.email()).isEqualTo("trainer@gymflow.dev");
        assertThat(principal.role()).isEqualTo(Role.TRAINER);
        assertThat(principal.gymId()).isEqualTo(gymId);
    }

    @Test
    void superAdminTokenHasNoGymId() {
        JwtService jwtService = new JwtService(properties(Duration.ofMinutes(15)));
        User user = new User(UUID.randomUUID(), "root@gymflow.dev", "irrelevant-hash", Role.SUPER_ADMIN, null, true, java.time.Instant.now());

        AuthenticatedPrincipal principal = jwtService.parseAccessToken(jwtService.issueAccessToken(user));

        assertThat(principal.gymId()).isNull();
    }

    @Test
    void expiredTokenFailsToParse() throws InterruptedException {
        JwtService jwtService = new JwtService(properties(Duration.ofMillis(1)));
        User user = new User(UUID.randomUUID(), "gone@gymflow.dev", "irrelevant-hash", Role.GYM_ADMIN, UUID.randomUUID(), true, java.time.Instant.now());

        String token = jwtService.issueAccessToken(user);
        Thread.sleep(20);

        assertThatThrownBy(() -> jwtService.parseAccessToken(token)).isInstanceOf(JwtException.class);
    }
}
