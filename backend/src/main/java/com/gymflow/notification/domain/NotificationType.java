package com.gymflow.notification.domain;

/** MEMBER_AT_RISK llegó en la Fase 14, cuando existió por fin el motor de detección (ver V11 migration para el CHECK constraint que lo habilita). */
public enum NotificationType {
    MEMBERSHIP_EXPIRING_SOON,
    MEMBER_AT_RISK
}
