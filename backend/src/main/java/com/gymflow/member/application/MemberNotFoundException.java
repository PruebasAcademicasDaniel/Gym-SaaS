package com.gymflow.member.application;

import com.gymflow.shared.error.NotFoundException;
import java.util.UUID;

/** Mismo mensaje si el socio no existe o si es de otro gimnasio (@TenantId ya lo hace invisible) — no hay distinción que dar. */
public class MemberNotFoundException extends NotFoundException {

    public MemberNotFoundException(UUID id) {
        super("No existe un socio con id " + id + ".");
    }
}
