package com.gymflow.auth.application;

import com.gymflow.shared.error.ConflictException;
import java.util.UUID;

public class MemberAlreadyHasPortalAccessException extends ConflictException {

    public MemberAlreadyHasPortalAccessException(UUID memberId) {
        super("El socio " + memberId + " ya tiene un acceso al portal.");
    }
}
