package com.gymflow.member;

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
import com.gymflow.member.infrastructure.web.UpdateMemberRequest;
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
 * Member es la primera entidad de negocio real con AbstractTenantEntity —
 * a diferencia de GymAndUserManagementIT (que probaba el filtro manual de
 * User), acá lo que se prueba es que el aislamiento funciona SIN ningún
 * código de filtrado escrito a mano: create/list/getById salen ya
 * acotados al gimnasio del actor solo por heredar de AbstractTenantEntity.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Testcontainers
class MemberIT {

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
    void gymAdmin_createsAndReadsAMember() {
        ResponseEntity<MemberResponse> created = createMember(gymAdminToken, "Carlos", "Pérez", "carlos@example.com", "555-1234");
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody().active()).isTrue();

        ResponseEntity<MemberResponse> fetched = restTemplate.exchange("/api/v1/members/" + created.getBody().id(), HttpMethod.GET,
                authenticated(gymAdminToken, null), MemberResponse.class);
        assertThat(fetched.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(fetched.getBody().firstName()).isEqualTo("Carlos");
    }

    @Test
    void trainer_canListAndReadButCannotCreate() {
        createMember(gymAdminToken, "Ana", "Gómez", null, null);

        ResponseEntity<String> forbidden = restTemplate.exchange("/api/v1/members", HttpMethod.POST,
                authenticated(trainerToken, new CreateMemberRequest("X", "Y", null, null)), String.class);
        assertThat(forbidden.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<MemberResponse[]> list = restTemplate.exchange("/api/v1/members", HttpMethod.GET,
                authenticated(trainerToken, null), MemberResponse[].class);
        assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(list.getBody()).extracting(MemberResponse::firstName).containsExactly("Ana");
    }

    @Test
    void gymAdmin_updatesAMember() {
        UUID memberId = createMember(gymAdminToken, "Laura", "Díaz", null, null).getBody().id();

        ResponseEntity<MemberResponse> updated = restTemplate.exchange("/api/v1/members/" + memberId, HttpMethod.PATCH,
                authenticated(gymAdminToken, new UpdateMemberRequest("Laura", "Díaz Actualizado", "laura@example.com", "555-9999")),
                MemberResponse.class);

        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updated.getBody().lastName()).isEqualTo("Díaz Actualizado");
        assertThat(updated.getBody().email()).isEqualTo("laura@example.com");
    }

    @Test
    void gymAdmin_deactivatesAMember() {
        UUID memberId = createMember(gymAdminToken, "Pedro", "Ruiz", null, null).getBody().id();

        ResponseEntity<Void> disableResponse = restTemplate.exchange("/api/v1/members/" + memberId + "/deactivate", HttpMethod.PATCH,
                authenticated(gymAdminToken, null), Void.class);
        assertThat(disableResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<MemberResponse> fetched = restTemplate.exchange("/api/v1/members/" + memberId, HttpMethod.GET,
                authenticated(gymAdminToken, null), MemberResponse.class);
        assertThat(fetched.getBody().active()).isFalse();
    }

    @Test
    void aGymNeverSeesAnotherGymsMembers_inListingsOrDirectLookup() {
        UUID myMemberId = createMember(gymAdminToken, "Local", "Uno", null, null).getBody().id();

        Gym otherGym = gymRepository.saveAndFlush(new Gym("Otro Gym", "otro-" + UUID.randomUUID()));
        String otherAdminEmail = "admin2-" + UUID.randomUUID() + "@gymflow.dev";
        userRepository.save(new User(otherAdminEmail, passwordEncoder.encode(PASSWORD), Role.GYM_ADMIN, otherGym.getId()));
        String otherAdminToken = login(otherAdminEmail, PASSWORD);
        UUID otherMemberId = createMember(otherAdminToken, "Ajeno", "Dos", null, null).getBody().id();

        ResponseEntity<MemberResponse[]> myList = restTemplate.exchange("/api/v1/members", HttpMethod.GET,
                authenticated(gymAdminToken, null), MemberResponse[].class);
        assertThat(myList.getBody()).extracting(MemberResponse::id).containsExactly(myMemberId);

        // no 403 — el otro socio directamente no existe para este tenant (@TenantId, no un chequeo manual).
        ResponseEntity<String> lookup = restTemplate.exchange("/api/v1/members/" + otherMemberId, HttpMethod.GET,
                authenticated(gymAdminToken, null), String.class);
        assertThat(lookup.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void memberCreationAndDeactivation_getAudited() {
        UUID memberId = createMember(gymAdminToken, "Sofía", "Torres", null, null).getBody().id();
        restTemplate.exchange("/api/v1/members/" + memberId + "/deactivate", HttpMethod.PATCH, authenticated(gymAdminToken, null), Void.class);

        var actions = auditLogRepository.findAll().stream()
                .filter(entry -> memberId.toString().equals(entry.getDetail()))
                .map(entry -> entry.getAction())
                .toList();
        assertThat(actions).containsExactlyInAnyOrder(AuditAction.MEMBER_CREATED, AuditAction.MEMBER_DEACTIVATED);
    }

    private ResponseEntity<MemberResponse> createMember(String token, String firstName, String lastName, String email, String phone) {
        return restTemplate.exchange("/api/v1/members", HttpMethod.POST,
                authenticated(token, new CreateMemberRequest(firstName, lastName, email, phone)), MemberResponse.class);
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
