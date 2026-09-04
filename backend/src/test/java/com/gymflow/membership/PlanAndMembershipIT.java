package com.gymflow.membership;

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
import java.math.BigDecimal;
import java.time.LocalDate;
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
 * Cubre la Fase 7 de punta a punta contra Postgres real: catálogo de
 * planes, contratación de membresías (con el cálculo de vencimiento),
 * cancelación, y — lo más importante para esta fase — que
 * MembershipService, al depender de MemberService/PlanService en vez de
 * sus repositorios, rechaza solo un memberId o planId de otro gimnasio
 * sin que el servicio tenga que chequear tenants explícitamente.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Testcontainers
class PlanAndMembershipIT {

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
    void gymAdmin_createsAPlan() {
        ResponseEntity<PlanResponse> response = createPlan(gymAdminToken, "Mensual", 30, "99.90");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().durationDays()).isEqualTo(30);
        assertThat(response.getBody().active()).isTrue();
    }

    @Test
    void trainer_canReadPlansButCannotCreate() {
        createPlan(gymAdminToken, "Anual", 365, "899.00");

        ResponseEntity<String> forbidden = restTemplate.exchange("/api/v1/plans", HttpMethod.POST,
                authenticated(trainerToken, new CreatePlanRequest("X", null, new BigDecimal("1.00"), 1)), String.class);
        assertThat(forbidden.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<PlanResponse[]> list =
                restTemplate.exchange("/api/v1/plans", HttpMethod.GET, authenticated(trainerToken, null), PlanResponse[].class);
        assertThat(list.getBody()).extracting(PlanResponse::name).containsExactly("Anual");
    }

    @Test
    void gymAdmin_contractsAMembership_endDateMatchesPlanDuration() {
        UUID planId = createPlan(gymAdminToken, "Mensual", 30, "99.90").getBody().id();
        UUID memberId = createMember(gymAdminToken, "Carlos", "Pérez").getBody().id();

        ResponseEntity<MembershipResponse> response = restTemplate.exchange("/api/v1/memberships", HttpMethod.POST,
                authenticated(gymAdminToken, new CreateMembershipRequest(memberId, planId)), MembershipResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().endDate()).isEqualTo(LocalDate.now().plusDays(30));
        assertThat(response.getBody().status()).isEqualTo("ACTIVE");
    }

    @Test
    void gymAdmin_seesTheMembersFullHistory_notJustTheActiveOne() {
        UUID memberId = createMember(gymAdminToken, "Laura", "Díaz").getBody().id();
        UUID plan1 = createPlan(gymAdminToken, "Mensual", 30, "99.90").getBody().id();
        UUID plan2 = createPlan(gymAdminToken, "Trimestral", 90, "249.00").getBody().id();
        createMembership(gymAdminToken, memberId, plan1);
        createMembership(gymAdminToken, memberId, plan2);

        ResponseEntity<MembershipResponse[]> history = restTemplate.exchange("/api/v1/members/" + memberId + "/memberships",
                HttpMethod.GET, authenticated(gymAdminToken, null), MembershipResponse[].class);

        assertThat(history.getBody()).hasSize(2);
    }

    @Test
    void cancellingAMembership_showsUpAsCancelled() {
        UUID planId = createPlan(gymAdminToken, "Mensual", 30, "99.90").getBody().id();
        UUID memberId = createMember(gymAdminToken, "Pedro", "Ruiz").getBody().id();
        UUID membershipId = createMembership(gymAdminToken, memberId, planId).getBody().id();

        ResponseEntity<Void> cancelResponse = restTemplate.exchange("/api/v1/memberships/" + membershipId + "/cancel", HttpMethod.PATCH,
                authenticated(gymAdminToken, null), Void.class);
        assertThat(cancelResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<MembershipResponse[]> history = restTemplate.exchange("/api/v1/members/" + memberId + "/memberships",
                HttpMethod.GET, authenticated(gymAdminToken, null), MembershipResponse[].class);
        assertThat(history.getBody()[0].status()).isEqualTo("CANCELLED");
    }

    @Test
    void cannotContractAMembership_forAnInactivePlan() {
        UUID planId = createPlan(gymAdminToken, "Descontinuado", 30, "50.00").getBody().id();
        restTemplate.exchange("/api/v1/plans/" + planId + "/deactivate", HttpMethod.PATCH, authenticated(gymAdminToken, null), Void.class);
        UUID memberId = createMember(gymAdminToken, "Sofía", "Torres").getBody().id();

        ResponseEntity<String> response = restTemplate.exchange("/api/v1/memberships", HttpMethod.POST,
                authenticated(gymAdminToken, new CreateMembershipRequest(memberId, planId)), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void cannotContractAMembership_usingAnotherGymsMemberOrPlan() {
        UUID myPlanId = createPlan(gymAdminToken, "Mensual", 30, "99.90").getBody().id();
        UUID myMemberId = createMember(gymAdminToken, "Local", "Uno").getBody().id();

        Gym otherGym = gymRepository.saveAndFlush(new Gym("Otro Gym", "otro-" + UUID.randomUUID()));
        String otherAdminEmail = "admin2-" + UUID.randomUUID() + "@gymflow.dev";
        userRepository.save(new User(otherAdminEmail, passwordEncoder.encode(PASSWORD), Role.GYM_ADMIN, otherGym.getId()));
        String otherAdminToken = login(otherAdminEmail, PASSWORD);
        UUID otherPlanId = createPlan(otherAdminToken, "Ajeno", 30, "10.00").getBody().id();
        UUID otherMemberId = createMember(otherAdminToken, "Ajeno", "Dos").getBody().id();

        ResponseEntity<String> crossMember = restTemplate.exchange("/api/v1/memberships", HttpMethod.POST,
                authenticated(gymAdminToken, new CreateMembershipRequest(otherMemberId, myPlanId)), String.class);
        assertThat(crossMember.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        ResponseEntity<String> crossPlan = restTemplate.exchange("/api/v1/memberships", HttpMethod.POST,
                authenticated(gymAdminToken, new CreateMembershipRequest(myMemberId, otherPlanId)), String.class);
        assertThat(crossPlan.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private ResponseEntity<PlanResponse> createPlan(String token, String name, int durationDays, String price) {
        return restTemplate.exchange("/api/v1/plans", HttpMethod.POST,
                authenticated(token, new CreatePlanRequest(name, null, new BigDecimal(price), durationDays)), PlanResponse.class);
    }

    private ResponseEntity<MemberResponse> createMember(String token, String firstName, String lastName) {
        return restTemplate.exchange("/api/v1/members", HttpMethod.POST,
                authenticated(token, new CreateMemberRequest(firstName, lastName, null, null)), MemberResponse.class);
    }

    private ResponseEntity<MembershipResponse> createMembership(String token, UUID memberId, UUID planId) {
        return restTemplate.exchange("/api/v1/memberships", HttpMethod.POST,
                authenticated(token, new CreateMembershipRequest(memberId, planId)), MembershipResponse.class);
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
