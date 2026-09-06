package com.gymflow.dashboard.application;

import com.gymflow.membership.application.MembershipService;
import com.gymflow.payment.application.PaymentService;
import com.gymflow.risk.application.RiskService;
import org.springframework.stereotype.Service;

/**
 * Puro agregador: no tiene entidad ni repositorio propios. Depende de
 * MembershipService/PaymentService/RiskService (capa de aplicación de esos
 * módulos), nunca de sus repositorios — mismo patrón de la Fase 7/8. Cada
 * número sale ya acotado al tenant del actor porque Membership/Payment son
 * AbstractTenantEntity (Fase 4/7/8) — este servicio no filtra nada.
 *
 * Ventana de "vencimiento próximo" fijada en 7 días: alinea con el
 * recordatorio de vencimiento que manda el módulo notification (Fase 12),
 * así el número que ve el admin acá es el mismo universo de socios a los
 * que después se les notifica. membersAtRisk (Fase 14) usa un criterio de
 * negocio distinto (días sin asistir, no días hasta el vencimiento) — su
 * propio umbral vive en RiskPolicy, no acá.
 */
@Service
public class DashboardService {

    private static final int EXPIRING_SOON_WINDOW_DAYS = 7;

    private final MembershipService membershipService;
    private final PaymentService paymentService;
    private final RiskService riskService;

    public DashboardService(MembershipService membershipService, PaymentService paymentService, RiskService riskService) {
        this.membershipService = membershipService;
        this.paymentService = paymentService;
        this.riskService = riskService;
    }

    public DashboardSummary getSummary() {
        return new DashboardSummary(
                membershipService.countActiveMembers(),
                membershipService.countExpiringWithinDays(EXPIRING_SOON_WINDOW_DAYS),
                paymentService.sumRevenueForCurrentMonth(),
                riskService.countAtRiskMembers());
    }
}
