package com.gymflow.shared.tenant;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.util.UUID;
import org.hibernate.annotations.TenantId;

/**
 * Base para entidades que pertenecen siempre a un único gimnasio, sin
 * excepción (Member, Plan, Membership, Payment, Attendance... a partir de
 * la Fase 6). Hibernate asigna gymId solo al persistir, a partir del
 * tenant actual (ver GymTenantIdentifierResolver) — no hay setter, así que
 * el código de aplicación no puede pisarlo ni por error.
 *
 * User NO extiende esto a propósito: necesita buscarse por email a través
 * de todos los tenants durante el login, y SUPER_ADMIN no tiene gymId. Ver
 * User.validateGymAssignment.
 *
 * Nota: una sesión de SUPER_ADMIN no ve filas de estas entidades a través
 * de los repositorios normales — su tenant resuelto es PLATFORM_TENANT_ID,
 * que ningún gimnasio real usa. Un panel cross-tenant para SUPER_ADMIN
 * (todavía no existe) va a necesitar su propio mecanismo explícito, no
 * este bypass automático.
 */
@MappedSuperclass
public abstract class AbstractTenantEntity {

    @TenantId
    @Column(name = "gym_id", nullable = false, updatable = false)
    private UUID gymId;

    public UUID getGymId() {
        return gymId;
    }
}
