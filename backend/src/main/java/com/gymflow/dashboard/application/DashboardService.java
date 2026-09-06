package com.gymflow.dashboard.application;

import com.gymflow.membership.application.MembershipService;
import com.gymflow.payment.application.PaymentService;
import org.springframework.stereotype.Service;

/**
 * Puro agregador: no tiene entidad ni repositorio propios. Depende de
 * MembershipService/PaymentService (capa de aplicación de esos módulos),
 * nunca de sus repositorios — mismo patrón de la Fase 7/8. Cada número sale
 * ya acotado al tenant del actor porque Membership/Payment son
 * AbstractTenantEntity (Fase 4/7/8) — este servicio no filtra nada.
 *
 * Ventana de "vencimiento próximo" fijada en 7 días: alinea con el
 * recordatorio de vencimiento que va a mandar el módulo notification en la
 * Fase 12, así el número que ve el admin acá es el mismo universo de
 * socios a los que después se les notifica.
 */
@Service
public class DashboardService {

    private static final int EXPIRING_SOON_WINDOW_DAYS = 7;

    private final MembershipService membershipService;
    private final PaymentService paymentService;

    public DashboardService(MembershipService membershipService, PaymentService paymentService) {
        this.membershipService = membershipService;
        this.paymentService = paymentService;
    }

    public DashboardSummary getSummary() {
        return new DashboardSummary(
                membershipService.countActiveMembers(),
                membershipService.countExpiringWithinDays(EXPIRING_SOON_WINDOW_DAYS),
                paymentService.sumRevenueForCurrentMonth());
    }
}
