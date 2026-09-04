package com.gymflow.auth.infrastructure.persistence;

import com.gymflow.auth.domain.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    /**
     * User no extiende AbstractTenantEntity (ver Fase 4), así que acá el
     * filtro por gimnasio es manual — a propósito, no un olvido.
     */
    List<User> findByGymId(UUID gymId);
}
