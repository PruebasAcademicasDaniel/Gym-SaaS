package com.gymflow.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.gymflow.auth.domain.Role;
import com.gymflow.auth.infrastructure.bootstrap.SuperAdminBootstrapper;
import com.gymflow.auth.infrastructure.persistence.UserRepository;
import com.gymflow.auth.infrastructure.web.LoginRequest;
import com.gymflow.auth.infrastructure.web.TokenResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Cubre la Fase 17: el único mecanismo para tener un SUPER_ADMIN fuera del
 * perfil dev. `SuperAdminBootstrapper` ya corrió una vez como
 * ApplicationRunner al levantar este contexto (con las propiedades de
 * abajo seteadas) — estos tests confirman el resultado de ESE arranque, y
 * llaman a run() de nuevo a mano para probar la idempotencia sin necesitar
 * reiniciar todo el contexto de Spring.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Testcontainers
class SuperAdminBootstrapperIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    private static final String EMAIL = "root@gymflow.test";
    private static final String PASSWORD = "BootstrapSecret123!";

    @DynamicPropertySource
    static void bootstrapProperties(DynamicPropertyRegistry registry) {
        registry.add("gymflow.bootstrap.super-admin-email", () -> EMAIL);
        registry.add("gymflow.bootstrap.super-admin-password", () -> PASSWORD);
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SuperAdminBootstrapper bootstrapper;

    @Test
    void theBootstrappedSuperAdminCanLogIn() {
        ResponseEntity<TokenResponse> response =
                restTemplate.postForEntity("/api/v1/auth/login", new LoginRequest(EMAIL, PASSWORD), TokenResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().accessToken()).isNotBlank();
    }

    @Test
    void runningTheBootstrapperAgain_doesNotCreateASecondSuperAdmin() {
        long before = userRepository.findAll().stream().filter(u -> u.getRole() == Role.SUPER_ADMIN).count();
        assertThat(before).isEqualTo(1);

        bootstrapper.run(null);

        long after = userRepository.findAll().stream().filter(u -> u.getRole() == Role.SUPER_ADMIN).count();
        assertThat(after).isEqualTo(1);
    }
}
