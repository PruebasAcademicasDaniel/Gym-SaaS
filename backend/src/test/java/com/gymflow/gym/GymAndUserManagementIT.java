package com.gymflow.gym;

import static org.assertj.core.api.Assertions.assertThat;

import com.gymflow.auth.domain.Role;
import com.gymflow.auth.domain.User;
import com.gymflow.auth.infrastructure.persistence.UserRepository;
import com.gymflow.auth.infrastructure.web.CreateUserRequest;
import com.gymflow.auth.infrastructure.web.LoginRequest;
import com.gymflow.auth.infrastructure.web.TokenResponse;
import com.gymflow.auth.infrastructure.web.UserResponse;
import com.gymflow.gym.domain.Gym;
import com.gymflow.gym.infrastructure.persistence.GymRepository;
import com.gymflow.gym.infrastructure.web.CreateGymRequest;
import com.gymflow.gym.infrastructure.web.GymResponse;
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
 * Cubre la Fase 5 de punta a punta: la FK nueva de app_user.gym_id, RBAC
 * por rol vía @PreAuthorize, y — lo más importante — que User (que no usa
 * el mecanismo automático de la Fase 4) queda igual de bien aislado por
 * gimnasio a mano: un GYM_ADMIN nunca ve, crea ni deshabilita usuarios de
 * otro gimnasio.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Testcontainers
class GymAndUserManagementIT {

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

    private String superAdminToken;
    private String gymAdminToken;
    private String gymAdminEmail;
    private UUID myGymId;

    @BeforeEach
    void seed() {
        String superEmail = "super-" + UUID.randomUUID() + "@gymflow.dev";
        userRepository.save(new User(superEmail, passwordEncoder.encode(PASSWORD), Role.SUPER_ADMIN, null));
        superAdminToken = login(superEmail, PASSWORD);

        Gym gym = gymRepository.saveAndFlush(new Gym("Mi Gimnasio", "gym-" + UUID.randomUUID()));
        myGymId = gym.getId();
        gymAdminEmail = "admin-" + UUID.randomUUID() + "@gymflow.dev";
        userRepository.save(new User(gymAdminEmail, passwordEncoder.encode(PASSWORD), Role.GYM_ADMIN, myGymId));
        gymAdminToken = login(gymAdminEmail, PASSWORD);
    }

    @Test
    void superAdmin_createsGym() {
        String slug = "acme-" + UUID.randomUUID();
        ResponseEntity<GymResponse> response = restTemplate.exchange("/api/v1/gyms", HttpMethod.POST,
                authenticated(superAdminToken, new CreateGymRequest("Acme Gym", slug)), GymResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().slug()).isEqualTo(slug);
    }

    @Test
    void gymAdmin_cannotCreateGym() {
        ResponseEntity<String> response = restTemplate.exchange("/api/v1/gyms", HttpMethod.POST,
                authenticated(gymAdminToken, new CreateGymRequest("Nope", "nope-" + UUID.randomUUID())), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void duplicateSlug_returnsConflict() {
        String slug = "dup-" + UUID.randomUUID();
        restTemplate.exchange("/api/v1/gyms", HttpMethod.POST, authenticated(superAdminToken, new CreateGymRequest("A", slug)), GymResponse.class);

        ResponseEntity<String> response = restTemplate.exchange("/api/v1/gyms", HttpMethod.POST,
                authenticated(superAdminToken, new CreateGymRequest("B", slug)), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void gymAdmin_getsOwnGym_butNotAnotherOnesById() {
        ResponseEntity<GymResponse> own = restTemplate.exchange("/api/v1/gyms/" + myGymId, HttpMethod.GET,
                authenticated(gymAdminToken, null), GymResponse.class);
        assertThat(own.getStatusCode()).isEqualTo(HttpStatus.OK);

        Gym otherGym = gymRepository.saveAndFlush(new Gym("Otro", "otro-" + UUID.randomUUID()));
        ResponseEntity<String> other = restTemplate.exchange("/api/v1/gyms/" + otherGym.getId(), HttpMethod.GET,
                authenticated(gymAdminToken, null), String.class);

        assertThat(other.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void gymAdmin_createsTrainer_andTheTrainerCanLogin() {
        String trainerEmail = "trainer-" + UUID.randomUUID() + "@gymflow.dev";

        ResponseEntity<UserResponse> created = restTemplate.exchange("/api/v1/users", HttpMethod.POST,
                authenticated(gymAdminToken, new CreateUserRequest(trainerEmail, "Trainer123!", Role.TRAINER, null, null)), UserResponse.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<TokenResponse> loginResponse =
                restTemplate.postForEntity("/api/v1/auth/login", new LoginRequest(trainerEmail, "Trainer123!"), TokenResponse.class);
        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void superAdmin_bootstrapsTheFirstGymAdminOfANewGym_andThatAdminCanLogin() {
        Gym newGym = gymRepository.saveAndFlush(new Gym("Gimnasio Nuevo", "nuevo-" + UUID.randomUUID()));
        String firstAdminEmail = "first-admin-" + UUID.randomUUID() + "@gymflow.dev";

        ResponseEntity<UserResponse> created = restTemplate.exchange("/api/v1/users", HttpMethod.POST,
                authenticated(superAdminToken, new CreateUserRequest(firstAdminEmail, "Secret123!", Role.GYM_ADMIN, newGym.getId(), null)),
                UserResponse.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<TokenResponse> loginResponse =
                restTemplate.postForEntity("/api/v1/auth/login", new LoginRequest(firstAdminEmail, "Secret123!"), TokenResponse.class);
        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void gymAdmin_cannotCreateSuperAdminThroughThisEndpoint() {
        ResponseEntity<String> response = restTemplate.exchange("/api/v1/users", HttpMethod.POST,
                authenticated(gymAdminToken, new CreateUserRequest("x-" + UUID.randomUUID() + "@gymflow.dev", "Secret123!", Role.SUPER_ADMIN, null, null)),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void gymAdmin_cannotEscapeItsOwnGymByPassingAnotherGymIdInTheBody() {
        Gym otherGym = gymRepository.saveAndFlush(new Gym("Otro Gym", "otro-" + UUID.randomUUID()));
        String email = "sneaky-" + UUID.randomUUID() + "@gymflow.dev";

        ResponseEntity<UserResponse> response = restTemplate.exchange("/api/v1/users", HttpMethod.POST,
                authenticated(gymAdminToken, new CreateUserRequest(email, "Secret123!", Role.TRAINER, otherGym.getId(), null)),
                UserResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        // el gymId del body se ignoró: quedó en el gimnasio del actor, no en otherGym.
        assertThat(userRepository.findByGymId(myGymId)).extracting(User::getEmail).contains(email);
        assertThat(userRepository.findByGymId(otherGym.getId())).extracting(User::getEmail).doesNotContain(email);
    }

    @Test
    void gymAdmin_listsOnlyItsOwnGymsUsers() {
        String myTrainerEmail = "trainer-" + UUID.randomUUID() + "@gymflow.dev";
        restTemplate.exchange("/api/v1/users", HttpMethod.POST,
                authenticated(gymAdminToken, new CreateUserRequest(myTrainerEmail, "Secret123!", Role.TRAINER, null, null)), UserResponse.class);

        Gym otherGym = gymRepository.saveAndFlush(new Gym("Otro Gym", "otro-" + UUID.randomUUID()));
        String otherAdminEmail = "admin2-" + UUID.randomUUID() + "@gymflow.dev";
        userRepository.save(new User(otherAdminEmail, passwordEncoder.encode(PASSWORD), Role.GYM_ADMIN, otherGym.getId()));
        String otherAdminToken = login(otherAdminEmail, PASSWORD);
        restTemplate.exchange("/api/v1/users", HttpMethod.POST,
                authenticated(otherAdminToken, new CreateUserRequest("trainer2-" + UUID.randomUUID() + "@gymflow.dev", "Secret123!", Role.TRAINER, null, null)),
                UserResponse.class);

        ResponseEntity<UserResponse[]> myUsers =
                restTemplate.exchange("/api/v1/users", HttpMethod.GET, authenticated(gymAdminToken, null), UserResponse[].class);

        // el propio GYM_ADMIN sembrado en @BeforeEach también es un usuario de este gimnasio.
        assertThat(myUsers.getBody()).extracting(UserResponse::email).containsExactlyInAnyOrder(gymAdminEmail, myTrainerEmail);
    }

    @Test
    void gymAdmin_disablesOwnUser_andTheDisabledUserCannotLoginAnymore() {
        String trainerEmail = "trainer-" + UUID.randomUUID() + "@gymflow.dev";
        ResponseEntity<UserResponse> created = restTemplate.exchange("/api/v1/users", HttpMethod.POST,
                authenticated(gymAdminToken, new CreateUserRequest(trainerEmail, "Secret123!", Role.TRAINER, null, null)), UserResponse.class);
        UUID trainerId = created.getBody().id();

        ResponseEntity<Void> disableResponse = restTemplate.exchange("/api/v1/users/" + trainerId + "/disable", HttpMethod.PATCH,
                authenticated(gymAdminToken, null), Void.class);
        assertThat(disableResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<String> loginResponse =
                restTemplate.postForEntity("/api/v1/auth/login", new LoginRequest(trainerEmail, "Secret123!"), String.class);
        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void gymAdmin_cannotDisableAUserFromAnotherGym() {
        Gym otherGym = gymRepository.saveAndFlush(new Gym("Otro Gym", "otro-" + UUID.randomUUID()));
        User victim = userRepository.save(new User(
                "victim-" + UUID.randomUUID() + "@gymflow.dev", passwordEncoder.encode(PASSWORD), Role.TRAINER, otherGym.getId()));

        ResponseEntity<String> response = restTemplate.exchange("/api/v1/users/" + victim.getId() + "/disable", HttpMethod.PATCH,
                authenticated(gymAdminToken, null), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
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
