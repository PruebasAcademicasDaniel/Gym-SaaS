package com.gymflow.auth.application;

import com.gymflow.auth.domain.Role;
import com.gymflow.auth.domain.User;
import com.gymflow.auth.infrastructure.persistence.UserRepository;
import com.gymflow.member.application.MemberService;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CRUD de usuarios internos de un gimnasio (GYM_ADMIN, TRAINER, y desde la
 * Fase 13 también MEMBER — el login de portal de un socio) — distinto de
 * AuthService, que es sobre iniciar sesión, no sobre administrar cuentas.
 * gymId siempre viene del actor autenticado, nunca del body: así un
 * GYM_ADMIN no puede crear ni tocar usuarios de otro gimnasio aunque lo
 * intente.
 */
@Service
public class UserManagementService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MemberService memberService;

    public UserManagementService(UserRepository userRepository, PasswordEncoder passwordEncoder, MemberService memberService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.memberService = memberService;
    }

    /**
     * memberId es obligatorio para role MEMBER e ignorado para cualquier
     * otro rol. No se valida "memberId pertenece a actingGymId" a mano:
     * memberService.getById() ya está acotado al tenant actual por
     * Hibernate (Fase 4) — un memberId de otro gimnasio, o uno que no
     * existe, sale como 404 sin código extra acá (mismo patrón que
     * MembershipService/PaymentService/AttendanceService).
     */
    @Transactional
    public User create(UUID actingGymId, String rawEmail, String rawPassword, Role role, UUID memberId) {
        if (role != Role.GYM_ADMIN && role != Role.TRAINER && role != Role.MEMBER) {
            throw new IllegalArgumentException("Este endpoint solo crea GYM_ADMIN, TRAINER o MEMBER, no " + role + ".");
        }

        String email = rawEmail.trim().toLowerCase(Locale.ROOT);
        if (userRepository.findByEmail(email).isPresent()) {
            throw new EmailAlreadyRegisteredException(email);
        }

        UUID linkedMemberId = null;
        if (role == Role.MEMBER) {
            memberService.getById(memberId); // 404 temprano si el socio no existe o es de otro gimnasio
            if (userRepository.findByMemberId(memberId).isPresent()) {
                throw new MemberAlreadyHasPortalAccessException(memberId);
            }
            linkedMemberId = memberId;
        }

        User user = new User(email, passwordEncoder.encode(rawPassword), role, actingGymId, linkedMemberId);
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
