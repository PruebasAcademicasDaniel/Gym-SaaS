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
 * Único lugar del proyecto donde se setea TenantContext manualmente fuera
 * de JwtAuthenticationFilter — automatizar recordatorios (pedido original
 * del producto, sección 1) necesita cruzar todos los gimnasios, algo que
 * ningún request HTTP normal hace. Es exactamente el "mecanismo explícito"
 * que las notas de la Fase 4/10 dejaron pendiente para un futuro caso
 * cross-tenant real — este es ese caso.
 *
 * try/finally por gimnasio, no uno solo al final del for: si un gimnasio
 * falla, el finally de ESE gimnasio limpia el contexto antes de pasar al
 * siguiente — sin eso, un error a mitad de loop dejaría el tenant de un
 * gimnasio pegado en el contexto para el resto de la iteración.
 */
@Component
public class ExpirationReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(ExpirationReminderScheduler.class);

    private final GymService gymService;
    private final NotificationService notificationService;

    public ExpirationReminderScheduler(GymService gymService, NotificationService notificationService) {
        this.gymService = gymService;
        this.notificationService = notificationService;
    }

    @Scheduled(cron = "0 0 7 * * *")
    public void run() {
        for (Gym gym : gymService.listAll()) {
            try {
                TenantContext.setCurrentTenantId(gym.getId());
                int sent = notificationService.sendExpirationReminders();
                log.info("Recordatorios de vencimiento — gimnasio {}: {} enviados", gym.getId(), sent);
            } catch (Exception ex) {
                log.error("Error enviando recordatorios para el gimnasio {}", gym.getId(), ex);
            } finally {
                TenantContext.clear();
            }
        }
    }
}
