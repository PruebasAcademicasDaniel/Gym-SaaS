package com.gymflow.auth.application;

import com.gymflow.shared.error.NotFoundException;
import java.util.UUID;

/**
 * Mismo mensaje tanto si el usuario no existe como si existe pero es de
 * otro gimnasio — no hay que darle a un atacante pistas sobre qué caso es.
 */
public class UserNotFoundException extends NotFoundException {

    public UserNotFoundException(UUID id) {
        super("No existe un usuario con id " + id + " en este gimnasio.");
    }
}
