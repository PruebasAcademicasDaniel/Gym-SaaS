package com.gymflow.auth.infrastructure.bootstrap;

import com.gymflow.auth.domain.Role;
import com.gymflow.auth.domain.User;
import com.gymflow.auth.infrastructure.persistence.UserRepository;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fase 17 (deploy): hasta esta fase, la ÚNICA forma de tener un SUPER_ADMIN
 * era el seed del perfil dev (db/migration-dev/V2), que nunca corre fuera
 * de ese perfil — una instalación de producción real arrancaría sin ningún
 * usuario, sin forma de loguearse ni de crear el primer gimnasio
 * (SUPER_ADMIN tampoco se puede crear por API, ver UserManagementService).
 * Este runner cierra ese hueco sin agregar un endpoint público de "crear
 * admin" (superficie de ataque innecesaria): solo lee credenciales del
 * entorno del propio servidor, nunca de una request.
 *
 * Seguro de dejar las variables de entorno seteadas para siempre: se
 * activa solo si SUPER_ADMIN_EMAIL/SUPER_ADMIN_PASSWORD están presentes Y
 * todavía no existe ningún SUPER_ADMIN — así que no crea uno nuevo (ni
 * falla) en reinicios posteriores.
 */
@Component
public class SuperAdminBootstrapper implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SuperAdminBootstrapper.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;

    public SuperAdminBootstrapper(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${gymflow.bootstrap.super-admin-email}") String email,
            @Value("${gymflow.bootstrap.super-admin-password}") String password) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            return; // sin las dos variables no hay nada que bootstrapear — mismo criterio "sin infraestructura hasta que haga falta" de toda la Fase 0
        }
        if (userRepository.existsByRole(Role.SUPER_ADMIN)) {
            return; // ya hay un SUPER_ADMIN — no crear otro, sin importar qué diga el entorno en este arranque
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            log.warn("SUPER_ADMIN_EMAIL ({}) ya está en uso por otro usuario — no se bootstrapeó ningún SUPER_ADMIN.", normalizedEmail);
            return;
        }

        userRepository.save(new User(normalizedEmail, passwordEncoder.encode(password), Role.SUPER_ADMIN, null));
        log.info("SUPER_ADMIN bootstrapeado para esta instalación: {}", normalizedEmail);
    }
}
