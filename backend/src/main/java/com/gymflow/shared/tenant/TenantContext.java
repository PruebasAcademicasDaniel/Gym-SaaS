package com.gymflow.shared.tenant;

import java.util.UUID;

/**
 * Tenant (gymId) del request actual, en un ThreadLocal. Solo lo puebla
 * JwtAuthenticationFilter a partir del claim firmado del JWT — nunca de un
 * parámetro o body de la request.
 *
 * Crítico: quien lo setea es responsable de limpiarlo en un finally al
 * terminar el request — Tomcat reutiliza threads entre requests, y un
 * valor que queda pegado se filtra al siguiente request de ese thread.
 */
public final class TenantContext {

    /**
     * Hibernate exige que el CurrentTenantIdentifierResolver nunca
     * devuelva null (si lo hace, falla incluso el arranque de la app con
     * "SessionFactory configured for multi-tenancy, but no tenant
     * identifier specified" — pasa ANTES de cualquier request, al validar
     * las queries de los repositorios). Este UUID reservado representa
     * "sin tenant real": arranque de la app, o un SUPER_ADMIN sin gymId.
     * Ningún gimnasio real tiene ni tendrá este id — así que cualquier
     * entidad @TenantId queda, en la práctica, con cero filas visibles
     * para una sesión en este estado. Ver GymTenantIdentifierResolver.
     */
    public static final UUID PLATFORM_TENANT_ID = new UUID(0L, 0L);

    private static final ThreadLocal<UUID> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void setCurrentTenantId(UUID gymId) {
        CURRENT_TENANT.set(gymId);
    }

    /** Nunca null — ver PLATFORM_TENANT_ID. */
    public static UUID getCurrentTenantId() {
        UUID value = CURRENT_TENANT.get();
        return value != null ? value : PLATFORM_TENANT_ID;
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
