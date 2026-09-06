package com.gymflow.risk.domain;

import java.time.LocalDate;

/**
 * La regla determinística en sí, aislada de Spring/JPA a propósito (Fase 0,
 * sección "testing": "JUnit 5 puro... motor de riesgo"). Un socio con
 * membresía activa está "en riesgo" si pasaron más de
 * INACTIVITY_THRESHOLD_DAYS días desde su última actividad conocida — el
 * check-in más reciente, o la fecha en que arrancó su membresía actual si
 * todavía no asistió nunca (ver RiskService.listAtRiskMembers()).
 *
 * El umbral es una constante propia de este módulo, deliberadamente
 * distinta de la ventana de "vencimiento próximo" (7 días, definida en
 * DashboardService y reutilizada por notification) — son dos conceptos de
 * negocio distintos y no hay razón para que compartan el número por
 * casualidad. 5 días es el criterio elegido para este MVP: se calibra con
 * datos reales de un gimnasio piloto antes de automatizar mensajes basados
 * en él (ver sección 16, riesgos técnicos, del documento de arquitectura).
 */
public final class RiskPolicy {

    public static final int INACTIVITY_THRESHOLD_DAYS = 5;

    private RiskPolicy() {
    }

    public static boolean isAtRisk(LocalDate lastActivity, LocalDate today) {
        return lastActivity.isBefore(today.minusDays(INACTIVITY_THRESHOLD_DAYS));
    }
}
