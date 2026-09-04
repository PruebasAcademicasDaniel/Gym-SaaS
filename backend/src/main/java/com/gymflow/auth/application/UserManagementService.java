package com.gymflow.auth.application;

import com.gymflow.auth.domain.Role;
import com.gymflow.auth.domain.User;
import com.gymflow.auth.infrastructure.persistence.UserRepository;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CRUD de usuarios internos de un gimnasio (GYM_ADMIN, TRAINER) — distinto
 * de AuthService, que es sobre iniciar sesión, no sobre administrar
 * cuentas. gymId siempre viene del actor autenticado, nunca del body: así
 * un GYM_ADMIN no puede crear ni tocar usuarios de otro gimnasio aunque lo
 * intente.
 */
@Service
public class UserManagementService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserManagementService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User create(UUID actingGymId, String rawEmail, String rawPassword, Role role) {
        if (role != Role.GYM_ADMIN && role != Role.TRAINER) {
            throw new IllegalArgumentException("Este endpoint solo crea GYM_ADMIN o TRAINER, no " + role + ".");
        }

        String email = rawEmail.trim().toLowerCase(Locale.ROOT);
        if (userRepository.findByEmail(email).isPresent()) {
            throw new EmailAlreadyRegisteredException(email);
        }

        User user = new User(email, passwordEncoder.encode(rawPassword), role, actingGymId);
        return userRepository.save(user);
    }

    public List<User> listByGym(UUID gymId) {
        return userRepository.findByGymId(gymId);
    }

    @Transactional
    public void disable(UUID actingGymId, UUID targetUserId) {
        User user = userRepository.findById(targetUserId)
                .filter(u -> actingGymId.equals(u.getGymId()))
                .orElseThrow(() -> new UserNotFoundException(targetUserId));
        user.disable();
        userRepository.save(user);
    }
}
