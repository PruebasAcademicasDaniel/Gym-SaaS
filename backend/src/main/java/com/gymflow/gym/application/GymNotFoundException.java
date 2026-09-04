package com.gymflow.gym.application;

import com.gymflow.shared.error.NotFoundException;
import java.util.UUID;

public class GymNotFoundException extends NotFoundException {

    public GymNotFoundException(UUID id) {
        super("No existe un gimnasio con id " + id + ".");
    }
}
