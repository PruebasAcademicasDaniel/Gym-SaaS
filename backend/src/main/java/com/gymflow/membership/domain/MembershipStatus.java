package com.gymflow.membership.domain;

/**
 * ACTIVE y CANCELLED son los únicos valores que la aplicación persiste
 * (cambian por una acción explícita). EXPIRED nunca se escribe — se
 * deriva comparando endDate contra hoy, ver Membership.getEffectiveStatus.
 */
public enum MembershipStatus {
    ACTIVE,
    EXPIRED,
    CANCELLED
}
