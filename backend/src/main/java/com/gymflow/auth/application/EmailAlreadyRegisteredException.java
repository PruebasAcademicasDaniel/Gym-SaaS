package com.gymflow.auth.application;

import com.gymflow.shared.error.ConflictException;

public class EmailAlreadyRegisteredException extends ConflictException {

    public EmailAlreadyRegisteredException(String email) {
        super("Ya existe un usuario con el email " + email + ".");
    }
}
