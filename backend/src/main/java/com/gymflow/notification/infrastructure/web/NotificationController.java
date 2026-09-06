package com.gymflow.notification.infrastructure.web;

import com.gymflow.notification.application.NotificationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Disparo manual, sobre el propio gimnasio del actor (TenantContext ya
 * resuelto por JwtAuthenticationFilter, como cualquier otro endpoint — sin
 * manejo especial). El scheduler automático diario (Fase 12, sección
 * "Automatizar recordatorios" del pedido original) es el otro disparador,
 * y ese sí cruza tenants explícitamente — ver ExpirationReminderScheduler.
 */
@RestController
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/api/v1/notifications/expiration-reminders")
    @PreAuthorize("hasRole('GYM_ADMIN')")
    public SendRemindersResponse sendExpirationReminders() {
        return new SendRemindersResponse(notificationService.sendExpirationReminders());
    }

    @PostMapping("/api/v1/notifications/risk-alerts")
    @PreAuthorize("hasRole('GYM_ADMIN')")
    public SendRemindersResponse sendRiskAlerts() {
        return new SendRemindersResponse(notificationService.sendRiskAlerts());
    }
}
