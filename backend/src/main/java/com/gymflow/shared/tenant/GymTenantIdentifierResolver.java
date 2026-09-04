package com.gymflow.shared.tenant;

import java.util.UUID;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;

/**
 * Lo que Hibernate consulta para saber a qué tenant restringir las
 * entidades @TenantId de la sesión actual. Nunca devuelve null —
 * TenantContext.getCurrentTenantId() ya resuelve a PLATFORM_TENANT_ID
 * cuando no hay tenant real (arranque de la app, SUPER_ADMIN). Devolver
 * null acá rompe hasta el arranque de la aplicación (Hibernate no tolera
 * un CurrentTenantIdentifierResolver que devuelva null, ni para entidades
 * no anotadas con @TenantId).
 */
@Component
public class GymTenantIdentifierResolver implements CurrentTenantIdentifierResolver<UUID> {

    @Override
    public UUID resolveCurrentTenantIdentifier() {
        return TenantContext.getCurrentTenantId();
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
}
