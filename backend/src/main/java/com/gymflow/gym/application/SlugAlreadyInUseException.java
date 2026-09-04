package com.gymflow.gym.application;

import com.gymflow.shared.error.ConflictException;

public class SlugAlreadyInUseException extends ConflictException {

    public SlugAlreadyInUseException(String slug) {
        super("El slug '" + slug + "' ya está en uso.");
    }
}
