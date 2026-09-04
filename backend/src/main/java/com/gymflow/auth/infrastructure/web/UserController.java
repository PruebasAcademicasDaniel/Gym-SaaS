package com.gymflow.auth.infrastructure.web;

import com.gymflow.auth.application.UserManagementService;
import com.gymflow.auth.domain.Role;
import com.gymflow.auth.domain.User;
import com.gymflow.auth.infrastructure.security.AuthenticatedPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Usuarios internos de un gimnasio. Para GYM_ADMIN, el gymId siempre sale
 * del token, nunca del body — no puede tocar otro gimnasio ni por error.
 * SUPER_ADMIN es la única excepción real: no tiene gymId propio, así que
 * al crear el primer GYM_ADMIN de un gimnasio nuevo sí usa el gymId que
 * manda en el body (ver create()).
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserManagementService userManagementService;

    public UserController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    /**
     * GYM_ADMIN crea dentro de su propio gimnasio (el gymId del body se
     * ignora si lo manda). SUPER_ADMIN es la única forma de dar de alta el
     * primer GYM_ADMIN de un gimnasio nuevo — ahí sí usa el gymId del body.
     */
    @PostMapping
    @PreAuthorize("hasRole('GYM_ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<UserResponse> create(
            @Valid @RequestBody CreateUserRequest request,
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        UUID targetGymId = principal.role() == Role.SUPER_ADMIN ? request.gymId() : principal.gymId();
        User user = userManagementService.create(targetGymId, request.email(), request.password(), request.role());
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user));
    }

    @GetMapping
    @PreAuthorize("hasRole('GYM_ADMIN')")
    public List<UserResponse> list(@AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return userManagementService.listByGym(principal.gymId()).stream().map(UserResponse::from).toList();
    }

    @PatchMapping("/{id}/disable")
    @PreAuthorize("hasRole('GYM_ADMIN')")
    public ResponseEntity<Void> disable(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        userManagementService.disable(principal.gymId(), id);
        return ResponseEntity.noContent().build();
    }
}
