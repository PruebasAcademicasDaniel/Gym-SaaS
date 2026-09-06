package com.gymflow.notification.application;

/**
 * Puerto (hexagonal) para el canal de envío. La única implementación hoy
 * es LoggingEmailSender — no hay SMTP configurado todavía porque no hace
 * falta infraestructura nueva para el MVP (Fase 0, sección 13). Cuando
 * exista un proveedor real, se agrega un adapter nuevo que implemente esta
 * misma interfaz — NotificationService no cambia una línea.
 */
public interface EmailSender {

    void send(String to, String subject, String body);
}
