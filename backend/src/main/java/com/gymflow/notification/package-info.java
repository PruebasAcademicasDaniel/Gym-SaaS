/**
 * Recordatorios de vencimiento próximo por email. El canal de envío es un
 * puerto (EmailSender) con un único adapter de logging por ahora — sin
 * SMTP real todavía, no hace falta esa infraestructura para el MVP.
 * "Cliente en riesgo" como tipo de notificación queda para la Fase 14,
 * cuando exista el motor que lo detecte.
 */
package com.gymflow.notification;
