package com.gymflow.membership.application;

import com.gymflow.shared.error.NotFoundException;
import java.util.UUID;

public class MembershipNotFoundException extends NotFoundException {

    public MembershipNotFoundException(UUID id) {
        super("No existe una membresía con id " + id + ".");
    }
}
