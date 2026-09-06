package com.gymflow.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import com.gymflow.auth.domain.Role;
import com.gymflow.auth.domain.User;
import com.gymflow.auth.infrastructure.persistence.UserRepository;
import com.gymflow.auth.infrastructure.web.LoginRequest;
import com.gymflow.auth.infrastructure.web.TokenResponse;
import com.gymflow.dashboard.infrastructure.web.DashboardResponse;
import com.gymflow.gym.domain.Gym;
import com.gymflow.gym.infrastructure.persistence.GymRepository;
import com.gymflow.member.infrastructure.web.CreateMemberRequest;
import com.gymflow.member.infrastructure.web.MemberResponse;
import com.gymflow.membership.infrastructure.web.CreateMembershipRequest;
import com.gymflow.membership.infrastructure.web.MembershipResponse;
import com.gymflow.payment.domain.PaymentMethod;
import com.gymflow.payment.infrastructure.web.CreatePaymentRequest;
import com.gymflow.plan.infrastructure.web.CreatePlanRequest;
import com.gymflow.plan.infrastructure.web.PlanResponse;
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
 * Cubre la Fase 10. Lo más importante de esta fase no es el cálculo en sí
 * — es confirmar que las queries JPQL agregadas nuevas (count/sum contra
 * Membership y Payment, entidades @TenantId) respetan el aislamiento de
 * tenant igual que los métodos derivados por nombre que ya se probaron en
 * fases anteriores. Sin esa garantía, un dashboard sería la primera fuga
 * de datos entre gimnasios del proyecto.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Testcontainers
class DashboardIT {

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
    void withNoData_summaryIsAllZero() {
        ResponseEntity<DashboardResponse> response =
                restTemplate.exchange("/api/v1/dashboard", HttpMethod.GET, authenticated(gymAdminToken), DashboardResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().activeMembers()).isZero();
        assertThat(response.getBody().membershipsExpiringSoon()).isZero();
        assertThat(response.getBody().revenueThisMonth()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getBody().membersAtRisk()).isZero();
    }

    @Test
    void trainer_hasNoAccessToTheDashboard() {
        ResponseEntity<String> response =
                restTemplate.exchange("/api/v1/dashboard", HttpMethod.GET, authenticated(trainerToken), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void activeMembers_countsDistinctMembersWithAnUnexpiredMembership() {
        UUID planId = createPlan(90, "50.00");
        UUID member1 = createMember("Ana", "Gómez");
        UUID member2 = createMember("Bruno", "Ríos");
        contract(member1, planId);
        contract(member2, planId);

        DashboardResponse summary = getSummary(gymAdminToken);

        assertThat(summary.activeMembers()).isEqualTo(2);
    }

    @Test
    void membershipsExpiringSoon_onlyCountsThoseWithinTheSevenDayWindow() {
        UUID longPlanId = createPlan(90, "50.00"); // vence lejos, no cuenta
        UUID shortPlanId = createPlan(3, "50.00"); // vence en 3 días, sí cuenta
        UUID memberFar = createMember("Lejos", "Uno");
        UUID memberSoon = createMember("Pronto", "Dos");
        contract(memberFar, longPlanId);
        contract(memberSoon, shortPlanId);

        DashboardResponse summary = getSummary(gymAdminToken);

        assertThat(summary.membershipsExpiringSoon()).isEqualTo(1);
    }

    @Test
    void revenueThisMonth_sumsOnlyThisMonthsPayments() {
        UUID planId = createPlan(30, "99.90");
        UUID memberId = createMember("Carlos", "Pérez");
        UUID membershipId = contract(memberId, planId);
        registerPayment(membershipId, "99.90");
        registerPayment(membershipId, "10.00");

        DashboardResponse summary = getSummary(gymAdminToken);

        assertThat(summary.revenueThisMonth()).isEqualByComparingTo("109.90");
    }

    @Test
    void aGymsDashboard_neverReflectsAnotherGymsData() {
        UUID planId = createPlan(3, "50.00");
        UUID memberId = createMember("Local", "Uno");
        UUID membershipId = contract(memberId, planId);
        registerPayment(membershipId, "50.00");

        Gym otherGym = gymRepository.saveAndFlush(new Gym("Otro Gym", "otro-" + UUID.randomUUID()));
        String otherAdminEmail = "admin2-" + UUID.randomUUID() + "@gymflow.dev";
        userRepository.save(new User(otherAdminEmail, passwordEncoder.encode(PASSWORD), Role.GYM_ADMIN, otherGym.getId()));
        String otherAdminToken = login(otherAdminEmail, PASSWORD);

        DashboardResponse otherSummary = getSummary(otherAdminToken);

        assertThat(otherSummary.activeMembers()).isZero();
        assertThat(otherSummary.membershipsExpiringSoon()).isZero();
        assertThat(otherSummary.revenueThisMonth()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(otherSummary.membersAtRisk()).isZero();
    }

    private UUID createPlan(int durationDays, String price) {
        return restTemplate.exchange("/api/v1/plans", HttpMethod.POST,
                authenticated(gymAdminToken, new CreatePlanRequest("Plan", null, new BigDecimal(price), durationDays)), PlanResponse.class)
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

    private void registerPayment(UUID membershipId, String amount) {
        restTemplate.exchange("/api/v1/payments", HttpMethod.POST,
                authenticated(gymAdminToken, new CreatePaymentRequest(membershipId, new BigDecimal(amount), PaymentMethod.CASH)), Void.class);
    }

    private DashboardResponse getSummary(String token) {
        return restTemplate.exchange("/api/v1/dashboard", HttpMethod.GET, authenticated(token), DashboardResponse.class).getBody();
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

    private HttpEntity<Void> authenticated(String token) {
        return authenticated(token, null);
    }
}
