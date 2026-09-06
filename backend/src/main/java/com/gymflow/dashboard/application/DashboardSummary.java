package com.gymflow.dashboard.application;

import java.math.BigDecimal;

/** membersAtRisk llegó en la Fase 14 — hasta entonces se dejó deliberadamente afuera por no existir todavía el motor de detección (ver RiskService). */
public record DashboardSummary(
        long activeMembers, long membershipsExpiringSoon, BigDecimal revenueThisMonth, long membersAtRisk) {
}
