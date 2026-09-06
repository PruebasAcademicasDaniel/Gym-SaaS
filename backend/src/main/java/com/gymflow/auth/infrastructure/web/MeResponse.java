package com.gymflow.auth.infrastructure.web;

import com.gymflow.auth.infrastructure.security.AuthenticatedPrincipal;
import java.util.UUID;

public record MeResponse(UUID userId, String email, String role, UUID gymId, UUID memberId) {

    public static MeResponse from(AuthenticatedPrincipal principal) {
        return new MeResponse(
                principal.userId(), principal.email(), principal.role().name(), principal.gymId(), principal.memberId());
    }
}
