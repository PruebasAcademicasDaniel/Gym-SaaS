package com.gymflow.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.gymflow.auth.domain.Role;
import com.gymflow.auth.domain.User;
import com.gymflow.auth.infrastructure.persistence.UserRepository;
import com.gymflow.auth.infrastructure.web.LoginRequest;
import com.gymflow.auth.infrastructure.web.MeResponse;
import com.gymflow.auth.infrastructure.web.RefreshRequest;
import com.gymflow.auth.infrastructure.web.TokenResponse;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Cubre el flujo completo de auth contra un Postgres real: login, acceso a
 * un endpoint protegido, rotación de refresh token y logout. Es el
 * equivalente de la Fase 3 al "test de aislamiento de tenant" de la Fase 4:
 * el que más importa, porque valida la mecánica de seguridad end to end.
 *
 * Las respuestas de error se piden como String (no TokenResponse): el body
 * en esos casos es un ProblemDetail, y deserializarlo contra un DTO de
 * éxito solo agrega ruido a la falla real que se está probando.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Testcontainers
class AuthFlowIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String email;
    private static final String PASSWORD = "Secret123!";

    @BeforeEach
    void seedUser() {
        email = "it-" + UUID.randomUUID() + "@gymflow.dev";
        userRepository.save(new User(email, passwordEncoder.encode(PASSWORD), Role.GYM_ADMIN, UUID.randomUUID()));
    }

    @Test
    void loginWithValidCredentials_returnsAccessAndRefreshTokens() {
        ResponseEntity<TokenResponse> response = login(email, PASSWORD);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().accessToken()).isNotBlank();
        assertThat(response.getBody().refreshToken()).isNotBlank();
        assertThat(response.getBody().tokenType()).isEqualTo("Bearer");
    }

    @Test
    void loginWithWrongPassword_returnsUnauthorized() {
        ResponseEntity<String> response = loginExpectingFailure(email, "wrong-password");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void loginWithUnknownEmail_returnsUnauthorized_notNotFound() {
        ResponseEntity<String> response = loginExpectingFailure("nobody-" + UUID.randomUUID() + "@gymflow.dev", PASSWORD);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void protectedEndpoint_withoutToken_returnsUnauthorized() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/v1/auth/me", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void protectedEndpoint_withValidAccessToken_returnsCurrentUser() {
        String accessToken = login(email, PASSWORD).getBody().accessToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        ResponseEntity<MeResponse> response =
                restTemplate.exchange("/api/v1/auth/me", HttpMethod.GET, new HttpEntity<>(headers), MeResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().email()).isEqualTo(email);
        assertThat(response.getBody().role()).isEqualTo("GYM_ADMIN");
    }

    @Test
    void refresh_rotatesToken_andInvalidatesThePreviousOne() {
        String firstRefreshToken = login(email, PASSWORD).getBody().refreshToken();

        ResponseEntity<TokenResponse> refreshed =
                restTemplate.postForEntity("/api/v1/auth/refresh", new RefreshRequest(firstRefreshToken), TokenResponse.class);
        assertThat(refreshed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(refreshed.getBody().refreshToken()).isNotEqualTo(firstRefreshToken);

        ResponseEntity<String> reuseOldToken =
                restTemplate.postForEntity("/api/v1/auth/refresh", new RefreshRequest(firstRefreshToken), String.class);
        assertThat(reuseOldToken.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void logout_revokesTheRefreshToken() {
        String refreshToken = login(email, PASSWORD).getBody().refreshToken();

        ResponseEntity<Void> logoutResponse =
                restTemplate.postForEntity("/api/v1/auth/logout", new RefreshRequest(refreshToken), Void.class);
        assertThat(logoutResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<String> refreshAfterLogout =
                restTemplate.postForEntity("/api/v1/auth/refresh", new RefreshRequest(refreshToken), String.class);
        assertThat(refreshAfterLogout.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private ResponseEntity<TokenResponse> login(String email, String password) {
        return restTemplate.postForEntity("/api/v1/auth/login", new LoginRequest(email, password), TokenResponse.class);
    }

    private ResponseEntity<String> loginExpectingFailure(String email, String password) {
        return restTemplate.postForEntity("/api/v1/auth/login", new LoginRequest(email, password), String.class);
    }
}
