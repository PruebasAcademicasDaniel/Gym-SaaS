package com.gymflow.notification.infrastructure.scheduling;

import com.gymflow.gym.application.GymService;
import com.gymflow.gym.domain.Gym;
import com.gymflow.notification.application.NotificationService;
import com.gymflow.shared.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Mismo patrón que ExpirationReminderScheduler (Fase 12) — TenantContext
 * seteado y limpiado por gimnasio, try/finally por iteración, no uno solo
 * al final del for. Clase separada (no un segundo método en la otra) a
 * propósito: son dos conceptos de negocio distintos con su propia ventana
 * de tiempo (7 AM vencimientos, 8 AM riesgo — corridas separadas, no
 * compiten por los mismos recursos ni se bloquean entre sí si una falla).
 */
@Component
public class RiskAlertScheduler {

    private static final Logger log = LoggerFactory.getLogger(RiskAlertScheduler.class);

    private final GymService gymService;
    private final NotificationService notificationService;

    public RiskAlertScheduler(GymService gymService, NotificationService notificationService) {
        this.gymService = gymService;
        this.notificationService = notificationService;
    }

    @Scheduled(cron = "0 0 8 * * *")
    public void run() {
        for (Gym gym : gymService.listAll()) {
            try {
                TenantContext.setCurrentTenantId(gym.getId());
                int sent = notificationService.sendRiskAlerts();
                log.info("Alertas de clientes en riesgo — gimnasio {}: {} enviadas", gym.getId(), sent);
            } catch (Exception ex) {
                log.error("Error enviando alertas de riesgo para el gimnasio {}", gym.getId(), ex);
            } finally {
                TenantContext.clear();
            }
        }
    }
}
