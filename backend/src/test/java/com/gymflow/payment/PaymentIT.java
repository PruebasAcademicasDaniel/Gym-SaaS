package com.gymflow.payment;

import static org.assertj.core.api.Assertions.assertThat;

import com.gymflow.audit.domain.AuditAction;
import com.gymflow.audit.infrastructure.persistence.AuditLogRepository;
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
import com.gymflow.payment.domain.PaymentMethod;
import com.gymflow.payment.infrastructure.web.CreatePaymentRequest;
import com.gymflow.payment.infrastructure.web.PaymentResponse;
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
 * Cubre la Fase 8: registro de pagos imputados a una membresía concreta,
 * sin acceso de TRAINER (a diferencia de member/plan — la matriz de
 * permisos de la Fase 0 no le da nada en Pagos), aislamiento entre
 * gimnasios vía MembershipService (mismo patrón que Fase 7), y que el
 * registro de un pago queda auditado.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Testcontainers
class PaymentIT {

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
    private AuditLogRepository auditLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String PASSWORD = "Secret123!";

    private String gymAdminToken;
    private String trainerToken;
    private UUID membershipId;

    @BeforeEach
    void seed() {
        Gym gym = gymRepository.saveAndFlush(new Gym("Mi Gimnasio", "gym-" + UUID.randomUUID()));

        String adminEmail = "admin-" + UUID.randomUUID() + "@gymflow.dev";
        userRepository.save(new User(adminEmail, passwordEncoder.encode(PASSWORD), Role.GYM_ADMIN, gym.getId()));
        gymAdminToken = login(adminEmail, PASSWORD);

        String trainerEmail = "trainer-" + UUID.randomUUID() + "@gymflow.dev";
        userRepository.save(new User(trainerEmail, passwordEncoder.encode(PASSWORD), Role.TRAINER, gym.getId()));
        trainerToken = login(trainerEmail, PASSWORD);

        UUID planId = restTemplate.exchange("/api/v1/plans", HttpMethod.POST,
                authenticated(gymAdminToken, new CreatePlanRequest("Mensual", null, new BigDecimal("99.90"), 30)), PlanResponse.class)
                .getBody().id();
        UUID memberId = restTemplate.exchange("/api/v1/members", HttpMethod.POST,
                authenticated(gymAdminToken, new CreateMemberRequest("Carlos", "Pérez", null, null)), MemberResponse.class)
                .getBody().id();
        membershipId = restTemplate.exchange("/api/v1/memberships", HttpMethod.POST,
                authenticated(gymAdminToken, new CreateMembershipRequest(memberId, planId)), MembershipResponse.class)
                .getBody().id();
    }

    @Test
    void gymAdmin_registersAPayment() {
        ResponseEntity<PaymentResponse> response = registerPayment(gymAdminToken, membershipId, "99.90", PaymentMethod.CASH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().amount()).isEqualByComparingTo("99.90");
        assertThat(response.getBody().method()).isEqualTo("CASH");
        assertThat(response.getBody().membershipId()).isEqualTo(membershipId);
    }

    @Test
    void trainer_hasNoAccessToPayments() {
        ResponseEntity<String> createResponse = restTemplate.exchange("/api/v1/payments", HttpMethod.POST,
                authenticated(trainerToken, new CreatePaymentRequest(membershipId, new BigDecimal("50.00"), PaymentMethod.CASH)), String.class);
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<String> listResponse = restTemplate.exchange("/api/v1/memberships/" + membershipId + "/payments", HttpMethod.GET,
                authenticated(trainerToken, null), String.class);
        assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void gymAdmin_listsPaymentsForAMembership() {
        registerPayment(gymAdminToken, membershipId, "99.90", PaymentMethod.CASH);
        registerPayment(gymAdminToken, membershipId, "10.00", PaymentMethod.CARD);

        ResponseEntity<PaymentResponse[]> list = restTemplate.exchange("/api/v1/memberships/" + membershipId + "/payments", HttpMethod.GET,
                authenticated(gymAdminToken, null), PaymentResponse[].class);

        assertThat(list.getBody()).hasSize(2);
    }

    @Test
    void cannotRegisterAPayment_forAnotherGymsMembership() {
        Gym otherGym = gymRepository.saveAndFlush(new Gym("Otro Gym", "otro-" + UUID.randomUUID()));
        String otherAdminEmail = "admin2-" + UUID.randomUUID() + "@gymflow.dev";
        userRepository.save(new User(otherAdminEmail, passwordEncoder.encode(PASSWORD), Role.GYM_ADMIN, otherGym.getId()));
        String otherAdminToken = login(otherAdminEmail, PASSWORD);

        ResponseEntity<String> response = restTemplate.exchange("/api/v1/payments", HttpMethod.POST,
                authenticated(otherAdminToken, new CreatePaymentRequest(membershipId, new BigDecimal("99.90"), PaymentMethod.CASH)),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void paymentRegistration_getsAudited() {
        UUID paymentId = registerPayment(gymAdminToken, membershipId, "99.90", PaymentMethod.TRANSFER).getBody().id();

        var actions = auditLogRepository.findAll().stream()
                .filter(entry -> paymentId.toString().equals(entry.getDetail()))
                .map(entry -> entry.getAction())
                .toList();

        assertThat(actions).containsExactly(AuditAction.PAYMENT_REGISTERED);
    }

    private ResponseEntity<PaymentResponse> registerPayment(String token, UUID membershipId, String amount, PaymentMethod method) {
        return restTemplate.exchange("/api/v1/payments", HttpMethod.POST,
                authenticated(token, new CreatePaymentRequest(membershipId, new BigDecimal(amount), method)), PaymentResponse.class);
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
