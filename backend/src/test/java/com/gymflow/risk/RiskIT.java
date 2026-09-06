package com.gymflow.risk;

import static org.assertj.core.api.Assertions.assertThat;

import com.gymflow.auth.domain.Role;
import com.gymflow.auth.domain.User;
import com.gymflow.auth.infrastructure.persistence.UserRepository;
import com.gymflow.auth.infrastructure.web.LoginRequest;
import com.gymflow.auth.infrastructure.web.TokenResponse;
import com.gymflow.gym.domain.Gym;
import com.gymflow.gym.infrastructure.persistence.GymRepository;
import com.gymflow.member.infrastructure.web.CreateMemberRequest;
import com.gymflow.member.infrastructure.web.MemberResponse;
import com.gymflow.membership.infrastructure.web.CreateMembershipRequest;
import com.gymflow.membership.infrastructure.web.MembershipResponse;
import com.gymflow.plan.infrastructure.web.CreatePlanRequest;
import com.gymflow.plan.infrastructure.web.PlanResponse;
import com.gymflow.risk.infrastructure.web.RiskMemberResponse;
import java.math.BigDecimal;
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
 * Cubre la Fase 14: wiring y permisos del endpoint, no la regla de negocio
 * en sí (esa la prueba RiskPolicyTest, un unit puro — no hay forma de
 * producir "5+ días sin asistir" a través de la API pública en un test que
 * corre en el momento, sin manipular el reloj o escribir fechas a mano en
 * el repositorio). Lo que sí es responsabilidad de este IT: que un socio
 * recién unido NUNCA aparece como falso positivo, que una membresía
 * cancelada deja de ser candidata, y que TRAINER no tiene acceso.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Testcontainers
class RiskIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GymRepository gymRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String PASSWORD = "Secret123!";

    private String gymAdminToken;
    private String trainerToken;

    @BeforeEach
    void seed() {
        Gym gym = gymRepository.saveAndFlush(new Gym("Mi Gimnasio", "gym-" + UUID.randomUUID()));

        String adminEmail = "admin-" + UUID.randomUUID() + "@gymflow.dev";
        userRepository.save(new User(adminEmail, passwordEncoder.encode(PASSWORD), Role.GYM_ADMIN, gym.getId()));
        gymAdminToken = login(adminEmail, PASSWORD);

        String trainerEmail = "trainer-" + UUID.randomUUID() + "@gymflow.dev";
        userRepository.save(new User(trainerEmail, passwordEncoder.encode(PASSWORD), Role.TRAINER, gym.getId()));
        trainerToken = login(trainerEmail, PASSWORD);
    }

    @Test
    void withNoActiveMemberships_noOneIsAtRisk() {
        ResponseEntity<RiskMemberResponse[]> response = listAtRisk(gymAdminToken);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void aMemberWhoJustJoined_isNotAtRiskYet() {
        UUID planId = createPlan(90);
        UUID memberId = createMember("Carlos", "Pérez");
        contract(memberId, planId);

        ResponseEntity<RiskMemberResponse[]> response = listAtRisk(gymAdminToken);

        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void aMemberWithACancelledMembership_isNeverConsideredAtRisk() {
        UUID planId = createPlan(90);
        UUID memberId = createMember("Ana", "Gómez");
        UUID membershipId = contract(memberId, planId);
        restTemplate.exchange("/api/v1/memberships/" + membershipId + "/cancel", HttpMethod.PATCH, authenticated(gymAdminToken, null), Void.class);

        ResponseEntity<RiskMemberResponse[]> response = listAtRisk(gymAdminToken);

        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void trainer_hasNoAccessToTheRiskEndpoint() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/risk/members", HttpMethod.GET, authenticated(trainerToken, null), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    private ResponseEntity<RiskMemberResponse[]> listAtRisk(String token) {
        return restTemplate.exchange("/api/v1/risk/members", HttpMethod.GET, authenticated(token, null), RiskMemberResponse[].class);
    }

    private UUID createPlan(int durationDays) {
        return restTemplate.exchange("/api/v1/plans", HttpMethod.POST,
                authenticated(gymAdminToken, new CreatePlanRequest("Plan", null, new BigDecimal("50.00"), durationDays)), PlanResponse.class)
                .getBody().id();
    }

    private UUID createMember(String firstName, String lastName) {
        return restTemplate.exchange("/api/v1/members", HttpMethod.POST,
                authenticated(gymAdminToken, new CreateMemberRequest(firstName, lastName, null, null)), MemberResponse.class)
                .getBody().id();
    }

    private UUID contract(UUID memberId, UUID planId) {
        return restTemplate.exchange("/api/v1/memberships", HttpMethod.POST,
                authenticated(gymAdminToken, new CreateMembershipRequest(memberId, planId)), MembershipResponse.class)
                .getBody().id();
    }

    private String login(String email, String password) {
        ResponseEntity<TokenResponse> response =
                restTemplate.postForEntity("/api/v1/auth/login", new LoginRequest(email, password), TokenResponse.class);
        return response.getBody().accessToken();
    }

    private <T> HttpEntity<T> authenticated(String token, T body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return new HttpEntity<>(body, headers);
    }
}
