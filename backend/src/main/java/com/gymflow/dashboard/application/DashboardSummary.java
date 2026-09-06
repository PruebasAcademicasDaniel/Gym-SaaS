package com.gymflow.dashboard.application;

import java.math.BigDecimal;

/**
 * Sin "clientes en riesgo" a propósito, aunque la Fase 0 lo menciona en el
 * MVP del dashboard: el motor de detección todavía no existe (Fase 14).
 * Publicar acá un número inventado sería peor que no publicar nada —
 * cuando el motor exista, este campo se agrega con un dato real, no antes.
 */
public record DashboardSummary(long activeMembers, long membershipsExpiringSoon, BigDecimal revenueThisMonth) {
}
