package com.gymflow.attendance;

import static org.assertj.core.api.Assertions.assertThat;

import com.gymflow.attendance.infrastructure.web.AttendanceResponse;
import com.gymflow.attendance.infrastructure.web.CheckInRequest;
import com.gymflow.auth.domain.Role;
import com.gymflow.auth.domain.User;
import com.gymflow.auth.infrastructure.persistence.UserRepository;
import com.gymflow.auth.infrastructure.web.LoginRequest;
import com.gymflow.auth.infrastructure.web.TokenResponse;
import com.gymflow.gym.domain.Gym;
import com.gymflow.gym.infrastructure.persistence.GymRepository;
import com.gymflow.member.infrastructure.web.CreateMemberRequest;
import com.gymflow.member.infrastructure.web.MemberResponse;
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
 * Cubre la Fase 9: check-in de socios. Lo distinto de esta fase respecto a
 * las anteriores es de permisos, no de arquitectura — TRAINER puede
 * escribir acá (a diferencia de member/plan/membership/payment, donde es
 * de solo lectura o no tiene acceso).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Testcontainers
class AttendanceIT {

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
    private UUID memberId;

    @BeforeEach
    void seed() {
        Gym gym = gymRepository.saveAndFlush(new Gym("Mi Gimnasio", "gym-" + UUID.randomUUID()));

        String adminEmail = "admin-" + UUID.randomUUID() + "@gymflow.dev";
        userRepository.save(new User(adminEmail, passwordEncoder.encode(PASSWORD), Role.GYM_ADMIN, gym.getId()));
        gymAdminToken = login(adminEmail, PASSWORD);

        String trainerEmail = "trainer-" + UUID.randomUUID() + "@gymflow.dev";
        userRepository.save(new User(trainerEmail, passwordEncoder.encode(PASSWORD), Role.TRAINER, gym.getId()));
        trainerToken = login(trainerEmail, PASSWORD);

        memberId = restTemplate.exchange("/api/v1/members", HttpMethod.POST,
                authenticated(gymAdminToken, new CreateMemberRequest("Carlos", "Pérez", null, null)), MemberResponse.class)
                .getBody().id();
    }

    @Test
    void trainer_canCheckInAMember() {
        ResponseEntity<AttendanceResponse> response = checkIn(trainerToken, memberId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().memberId()).isEqualTo(memberId);
    }

    @Test
    void gymAdmin_canAlsoCheckInAMember() {
        ResponseEntity<AttendanceResponse> response = checkIn(gymAdminToken, memberId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void multipleCheckInsOnTheSameDay_areAllRecorded_noDeduplication() {
        checkIn(gymAdminToken, memberId);
        checkIn(gymAdminToken, memberId);
        checkIn(gymAdminToken, memberId);

        ResponseEntity<AttendanceResponse[]> history = restTemplate.exchange("/api/v1/members/" + memberId + "/attendance",
                HttpMethod.GET, authenticated(gymAdminToken, null), AttendanceResponse[].class);

        assertThat(history.getBody()).hasSize(3);
    }

    @Test
    void cannotCheckInAMember_fromAnotherGym() {
        Gym otherGym = gymRepository.saveAndFlush(new Gym("Otro Gym", "otro-" + UUID.randomUUID()));
        String otherAdminEmail = "admin2-" + UUID.randomUUID() + "@gymflow.dev";
        userRepository.save(new User(otherAdminEmail, passwordEncoder.encode(PASSWORD), Role.GYM_ADMIN, otherGym.getId()));
        String otherAdminToken = login(otherAdminEmail, PASSWORD);
        UUID otherMemberId = restTemplate.exchange("/api/v1/members", HttpMethod.POST,
                authenticated(otherAdminToken, new CreateMemberRequest("Ajeno", "Dos", null, null)), MemberResponse.class)
                .getBody().id();

        ResponseEntity<String> response = restTemplate.exchange("/api/v1/attendance", HttpMethod.POST,
                authenticated(gymAdminToken, new CheckInRequest(otherMemberId)), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private ResponseEntity<AttendanceResponse> checkIn(String token, UUID memberId) {
        return restTemplate.exchange("/api/v1/attendance", HttpMethod.POST,
                authenticated(token, new CheckInRequest(memberId)), AttendanceResponse.class);
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
