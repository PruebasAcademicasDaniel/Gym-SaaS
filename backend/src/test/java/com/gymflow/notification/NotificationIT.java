package com.gymflow.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

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
import com.gymflow.notification.application.EmailSender;
import com.gymflow.notification.infrastructure.scheduling.ExpirationReminderScheduler;
import com.gymflow.notification.infrastructure.scheduling.RiskAlertScheduler;
import com.gymflow.notification.infrastructure.web.SendRemindersResponse;
import com.gymflow.plan.infrastructure.web.CreatePlanRequest;
import com.gymflow.plan.infrastructure.web.PlanResponse;
import com.gymflow.shared.tenant.TenantContext;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Cubre la Fase 12: el disparo manual (por gimnasio, vía HTTP) y el
 * scheduler automático (cross-tenant, sin HTTP de por medio). El caso que
 * más importa es el del scheduler: prueba que TenantContext se setea y
 * limpia gimnasio por gimnasio sin filtrarse entre ellos — es el primer
 * lugar del proyecto donde se maneja a mano fuera de
 * JwtAuthenticationFilter.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Testcontainers
class NotificationIT {

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

    @Autowired
    private ExpirationReminderScheduler scheduler;

    @Autowired
    private RiskAlertScheduler riskAlertScheduler;

    @MockitoBean
    private EmailSender emailSender;

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
    void gymAdmin_sendsReminders_onlyForMembersWithEmailExpiringWithinTheWindow() {
        UUID shortPlanId = createPlan(gymAdminToken, 3, "50.00"); // vence pronto — sí cuenta
        UUID longPlanId = createPlan(gymAdminToken, 90, "50.00"); // vence lejos — no cuenta
        UUID withEmailMember = createMember(gymAdminToken, "Ana", "Gómez", "ana@example.com");
        UUID withoutEmailMember = createMember(gymAdminToken, "Bruno", "Ríos", null);
        UUID farMember = createMember(gymAdminToken, "Lejos", "Uno", "lejos@example.com");
        contract(gymAdminToken, withEmailMember, shortPlanId);
        contract(gymAdminToken, withoutEmailMember, shortPlanId);
        contract(gymAdminToken, farMember, longPlanId);

        ResponseEntity<SendRemindersResponse> response = restTemplate.exchange(
                "/api/v1/notifications/expiration-reminders", HttpMethod.POST, authenticated(gymAdminToken), SendRemindersResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().sent()).isEqualTo(1);
        verify(emailSender).send(eq("ana@example.com"), anyString(), anyString());
        verifyNoMoreInteractions(emailSender);
    }

    @Test
    void sendingRemindersTwice_doesNotNotifyTheSameMembershipAgain() {
        UUID planId = createPlan(gymAdminToken, 3, "50.00");
        UUID memberId = createMember(gymAdminToken, "Carlos", "Pérez", "carlos@example.com");
        contract(gymAdminToken, memberId, planId);

        restTemplate.exchange("/api/v1/notifications/expiration-reminders", HttpMethod.POST, authenticated(gymAdminToken), SendRemindersResponse.class);
        ResponseEntity<SendRemindersResponse> second = restTemplate.exchange(
                "/api/v1/notifications/expiration-reminders", HttpMethod.POST, authenticated(gymAdminToken), SendRemindersResponse.class);

        assertThat(second.getBody().sent()).isZero();
        verify(emailSender, times(1)).send(any(), any(), any());
    }

    @Test
    void trainer_cannotTriggerReminders() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/notifications/expiration-reminders", HttpMethod.POST, authenticated(trainerToken), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void scheduler_processesEveryGymWithoutLeakingTenantContextBetweenThem() {
        UUID planA = createPlan(gymAdminToken, 3, "50.00");
        UUID memberA = createMember(gymAdminToken, "Local", "Uno", "local@example.com");
        contract(gymAdminToken, memberA, planA);

        Gym gymB = gymRepository.saveAndFlush(new Gym("Otro Gym", "otro-" + UUID.randomUUID()));
        String otherAdminEmail = "admin2-" + UUID.randomUUID() + "@gymflow.dev";
        userRepository.save(new User(otherAdminEmail, passwordEncoder.encode(PASSWORD), Role.GYM_ADMIN, gymB.getId()));
        String otherAdminToken = login(otherAdminEmail, PASSWORD);
        UUID planB = createPlan(otherAdminToken, 3, "50.00");
        UUID memberB = createMember(otherAdminToken, "Ajeno", "Dos", "ajeno@example.com");
        contract(otherAdminToken, memberB, planB);

        scheduler.run();

        // el propio scheduler siempre limpia TenantContext al final de cada gimnasio, sin excepción.
        assertThat(TenantContext.getCurrentTenantId()).isEqualTo(TenantContext.PLATFORM_TENANT_ID);

        ArgumentCaptor<String> toCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailSender, times(2)).send(toCaptor.capture(), any(), any());
        assertThat(toCaptor.getAllValues()).containsExactlyInAnyOrder("local@example.com", "ajeno@example.com");

        // cada gimnasio ve su propio recordatorio como ya enviado — prueba que Notification quedó atribuida al tenant correcto.
        ResponseEntity<SendRemindersResponse> gymAAgain = restTemplate.exchange(
                "/api/v1/notifications/expiration-reminders", HttpMethod.POST, authenticated(gymAdminToken), SendRemindersResponse.class);
        ResponseEntity<SendRemindersResponse> gymBAgain = restTemplate.exchange(
                "/api/v1/notifications/expiration-reminders", HttpMethod.POST, authenticated(otherAdminToken), SendRemindersResponse.class);
        assertThat(gymAAgain.getBody().sent()).isZero();
        assertThat(gymBAgain.getBody().sent()).isZero();
    }

    @Test
    void gymAdmin_sendsRiskAlerts_returnsZeroWhenNobodyIsAtRisk() {
        // Sin forma de producir "5+ días sin asistir" a través de la API en un test que corre en el momento — ver RiskIT.
        // Esto prueba wiring/idempotencia con cero candidatos, no la regla de negocio en sí (esa la prueba RiskPolicyTest).
        UUID planId = createPlan(gymAdminToken, 90, "50.00");
        UUID memberId = createMember(gymAdminToken, "Recién", "Llegado", "recien@example.com");
        contract(gymAdminToken, memberId, planId);

        ResponseEntity<SendRemindersResponse> response = restTemplate.exchange(
                "/api/v1/notifications/risk-alerts", HttpMethod.POST, authenticated(gymAdminToken), SendRemindersResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().sent()).isZero();
        verifyNoMoreInteractions(emailSender);
    }

    @Test
    void trainer_cannotTriggerRiskAlerts() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/notifications/risk-alerts", HttpMethod.POST, authenticated(trainerToken), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void riskAlertScheduler_processesEveryGymWithoutLeakingTenantContext() {
        riskAlertScheduler.run();

        assertThat(TenantContext.getCurrentTenantId()).isEqualTo(TenantContext.PLATFORM_TENANT_ID);
    }

    private UUID createPlan(String token, int durationDays, String price) {
        return restTemplate.exchange("/api/v1/plans", HttpMethod.POST,
                authenticated(token, new CreatePlanRequest("Plan", null, new BigDecimal(price), durationDays)), PlanResponse.class)
                .getBody().id();
    }

    private UUID createMember(String token, String firstName, String lastName, String email) {
        return restTemplate.exchange("/api/v1/members", HttpMethod.POST,
                authenticated(token, new CreateMemberRequest(firstName, lastName, email, null)), MemberResponse.class)
                .getBody().id();
    }

    private UUID contract(String token, UUID memberId, UUID planId) {
        return restTemplate.exchange("/api/v1/memberships", HttpMethod.POST,
                authenticated(token, new CreateMembershipRequest(memberId, planId)), MembershipResponse.class)
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

    private HttpEntity<Void> authenticated(String token) {
        return authenticated(token, null);
    }
}
