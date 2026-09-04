package com.gymflow.plan.application;

import com.gymflow.shared.error.NotFoundException;
import java.util.UUID;

public class PlanNotFoundException extends NotFoundException {

    public PlanNotFoundException(UUID id) {
        super("No existe un plan con id " + id + ".");
    }
}
