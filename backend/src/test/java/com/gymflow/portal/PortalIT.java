package com.gymflow.portal;

import static org.assertj.core.api.Assertions.assertThat;

import com.gymflow.attendance.infrastructure.web.AttendanceResponse;
import com.gymflow.attendance.infrastructure.web.CheckInRequest;
import com.gymflow.auth.domain.Role;
import com.gymflow.auth.domain.User;
import com.gymflow.auth.infrastructure.persistence.UserRepository;
import com.gymflow.auth.infrastructure.web.CreateUserRequest;
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
import com.gymflow.portal.infrastructure.web.PortalMembershipResponse;
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
 * Cubre la Fase 13: portal de autoservicio para MEMBER. Lo que más importa
 * probar acá no es "el CRUD funciona" (eso ya lo cubren los IT de cada
 * módulo) sino dos cosas nuevas de esta fase: (1) el vínculo User↔Member
 * recién creado (alta de acceso al portal, un socio no puede tener dos
 * logins, un memberId de otro gimnasio se rechaza), y (2) que un MEMBER
 * solo puede ver SU PROPIO socio — el filtro de tenant (Fase 4) no alcanza
 * acá, porque dos socios del mismo gimnasio comparten tenant.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Testcontainers
class PortalIT {

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
    private static final String PORTAL_PASSWORD = "SocioSecret123!";

    private String gymAdminToken;
    private UUID gymId;
    private UUID carlosId;
    private UUID membershipId;

    @BeforeEach
    void seed() {
        Gym gym = gymRepository.saveAndFlush(new Gym("Mi Gimnasio", "gym-" + UUID.randomUUID()));
        gymId = gym.getId();

        String adminEmail = "admin-" + UUID.randomUUID() + "@gymflow.dev";
        userRepository.save(new User(adminEmail, passwordEncoder.encode(PASSWORD), Role.GYM_ADMIN, gymId));
        gymAdminToken = login(adminEmail, PASSWORD);

        carlosId = restTemplate.exchange("/api/v1/members", HttpMethod.POST,
                authenticated(gymAdminToken, new CreateMemberRequest("Carlos", "Pérez", "carlos@example.com", null)),
                MemberResponse.class).getBody().id();

        UUID planId = restTemplate.exchange("/api/v1/plans", HttpMethod.POST,
                authenticated(gymAdminToken, new CreatePlanRequest("Plan Mensual", "desc", new BigDecimal("15000"), 30)),
                PlanResponse.class).getBody().id();

        membershipId = restTemplate.exchange("/api/v1/memberships", HttpMethod.POST,
                authenticated(gymAdminToken, new CreateMembershipRequest(carlosId, planId)),
                MembershipResponse.class).getBody().id();

        restTemplate.exchange("/api/v1/payments", HttpMethod.POST,
                authenticated(gymAdminToken, new CreatePaymentRequest(membershipId, new BigDecimal("15000"), PaymentMethod.CASH)),
                PaymentResponse.class);

        restTemplate.exchange("/api/v1/attendance", HttpMethod.POST,
                authenticated(gymAdminToken, new CheckInRequest(carlosId)), AttendanceResponse.class);
    }

    @Test
    void gymAdmin_canGrantPortalAccess_andTheMemberCanLogIn() {
        String email = "carlos-" + UUID.randomUUID() + "@example.com";

        ResponseEntity<String> response = grantPortalAccess(email, carlosId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(login(email, PORTAL_PASSWORD)).isNotBlank();
    }

    @Test
    void aMember_seesOnlyTheirOwnProfileMembershipsPaymentsAndAttendance() {
        String email = "carlos-" + UUID.randomUUID() + "@example.com";
        grantPortalAccess(email, carlosId);
        String memberToken = login(email, PORTAL_PASSWORD);

        ResponseEntity<MemberResponse> me = restTemplate.exchange("/api/v1/portal/me", HttpMethod.GET,
                authenticated(memberToken, null), MemberResponse.class);
        assertThat(me.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(me.getBody().id()).isEqualTo(carlosId);
        assertThat(me.getBody().firstName()).isEqualTo("Carlos");

        ResponseEntity<PortalMembershipResponse[]> memberships = restTemplate.exchange("/api/v1/portal/memberships",
                HttpMethod.GET, authenticated(memberToken, null), PortalMembershipResponse[].class);
        assertThat(memberships.getBody()).hasSize(1);
        assertThat(memberships.getBody()[0].id()).isEqualTo(membershipId);
        assertThat(memberships.getBody()[0].planName()).isEqualTo("Plan Mensual");

        ResponseEntity<PaymentResponse[]> payments = restTemplate.exchange("/api/v1/portal/payments",
                HttpMethod.GET, authenticated(memberToken, null), PaymentResponse[].class);
        assertThat(payments.getBody()).hasSize(1);
        assertThat(payments.getBody()[0].amount()).isEqualByComparingTo("15000");

        ResponseEntity<AttendanceResponse[]> attendance = restTemplate.exchange("/api/v1/portal/attendance",
                HttpMethod.GET, authenticated(memberToken, null), AttendanceResponse[].class);
        assertThat(attendance.getBody()).hasSize(1);
        assertThat(attendance.getBody()[0].memberId()).isEqualTo(carlosId);
    }

    @Test
    void aMember_neverSeesAnotherMembersDataInTheSameGym() {
        UUID anaId = restTemplate.exchange("/api/v1/members", HttpMethod.POST,
                authenticated(gymAdminToken, new CreateMemberRequest("Ana", "Gómez", "ana@example.com", null)),
                MemberResponse.class).getBody().id();
        String anaEmail = "ana-" + UUID.randomUUID() + "@example.com";
        grantPortalAccess(anaEmail, anaId);
        String anaToken = login(anaEmail, PORTAL_PASSWORD);

        ResponseEntity<MemberResponse> me = restTemplate.exchange("/api/v1/portal/me", HttpMethod.GET,
                authenticated(anaToken, null), MemberResponse.class);
        assertThat(me.getBody().id()).isEqualTo(anaId);
        assertThat(me.getBody().id()).isNotEqualTo(carlosId);

        ResponseEntity<PortalMembershipResponse[]> anaMemberships = restTemplate.exchange("/api/v1/portal/memberships",
                HttpMethod.GET, authenticated(anaToken, null), PortalMembershipResponse[].class);
        assertThat(anaMemberships.getBody()).isEmpty(); // Ana no contrató ningún plan — no ve la membresía de Carlos
    }

    @Test
    void aMember_getsForbidden_onAdminEndpoints() {
        String email = "carlos-" + UUID.randomUUID() + "@example.com";
        grantPortalAccess(email, carlosId);
        String memberToken = login(email, PORTAL_PASSWORD);

        ResponseEntity<String> response = restTemplate.exchange("/api/v1/members", HttpMethod.GET,
                authenticated(memberToken, null), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void aMemberCannotHaveTwoPortalLogins() {
        grantPortalAccess("carlos-" + UUID.randomUUID() + "@example.com", carlosId);

        ResponseEntity<String> second = grantPortalAccess("carlos-otro-" + UUID.randomUUID() + "@example.com", carlosId);

        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void grantingPortalAccess_forAMemberFromAnotherGym_isRejected() {
        Gym otherGym = gymRepository.saveAndFlush(new Gym("Otro Gym", "otro-" + UUID.randomUUID()));
        String otherAdminEmail = "admin2-" + UUID.randomUUID() + "@gymflow.dev";
        userRepository.save(new User(otherAdminEmail, passwordEncoder.encode(PASSWORD), Role.GYM_ADMIN, otherGym.getId()));
        String otherAdminToken = login(otherAdminEmail, PASSWORD);
        UUID otherMemberId = restTemplate.exchange("/api/v1/members", HttpMethod.POST,
                authenticated(otherAdminToken, new CreateMemberRequest("Ajeno", "Dos", null, null)), MemberResponse.class)
                .getBody().id();

        // gymAdmin (del primer gimnasio) intenta dar acceso de portal a un socio del OTRO gimnasio.
        ResponseEntity<String> response = grantPortalAccess("intruso-" + UUID.randomUUID() + "@example.com", otherMemberId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private ResponseEntity<String> grantPortalAccess(String email, UUID memberId) {
        return restTemplate.exchange("/api/v1/users", HttpMethod.POST,
                authenticated(gymAdminToken, new CreateUserRequest(email, PORTAL_PASSWORD, Role.MEMBER, null, memberId)),
                String.class);
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
