package com.gymflow.auth.domain;

/**
 * Roles del MVP (ver Fase 0, sección 6). GYM_OWNER y RECEPTIONIST quedaron
 * fuera del MVP; se agregan cuando exista billing y equipos más grandes.
 */
public enum Role {
    SUPER_ADMIN,
    GYM_ADMIN,
    TRAINER,
    MEMBER
}
