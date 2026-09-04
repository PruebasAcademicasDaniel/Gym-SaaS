package com.gymflow.auth.infrastructure.web;

import com.gymflow.auth.domain.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * gymId solo lo usa un SUPER_ADMIN (para dar de alta al primer GYM_ADMIN
 * de un gimnasio nuevo — sin esto no habría forma de arrancar un gimnasio).
 * Si lo manda un GYM_ADMIN, se ignora: su propio gymId manda siempre — ver
 * UserController.
 */
public record CreateUserRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, message = "mínimo 8 caracteres") String password,
        @NotNull Role role,
        UUID gymId) {
}
