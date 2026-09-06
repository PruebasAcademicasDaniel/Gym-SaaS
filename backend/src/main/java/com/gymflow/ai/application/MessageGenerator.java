package com.gymflow.ai.application;

/**
 * Puerto (hexagonal) para redactar el contenido de un aviso a un cliente
 * en riesgo. NotificationService depende de esto, nunca de una
 * implementación concreta — igual que con EmailSender (Fase 12).
 */
public interface MessageGenerator {

    GeneratedMessage generateRiskAlert(RiskAlertContext context);
}
