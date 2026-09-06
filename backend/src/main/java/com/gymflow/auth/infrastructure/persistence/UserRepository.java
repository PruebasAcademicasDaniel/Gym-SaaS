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

    /** Fase 13: para saber si un socio ya tiene un login de portal antes de crear otro (un socio → a lo sumo un login). */
    Optional<User> findByMemberId(UUID memberId);
}
