package com.gymflow.auth.infrastructure.security;

import com.gymflow.auth.domain.Role;
import java.util.UUID;

/**
 * Lo que queda en el SecurityContext tras validar un access token. Se arma
 * enteramente a partir de los claims del JWT — no pega contra la base en
 * cada request.
 */
public record AuthenticatedPrincipal(UUID userId, String email, Role role, UUID gymId, UUID memberId) {
}
