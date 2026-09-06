/**
 * Capa de IA sobre el motor de riesgo (Fase 15) — redacta el contenido de
 * los avisos a clientes en riesgo, nunca decide quién está en riesgo (eso
 * sigue siendo responsabilidad exclusiva de risk.domain.RiskPolicy, una
 * regla determinística y explicable). Puerto/adaptador (hexagonal), mismo
 * patrón que notification.application.EmailSender (Fase 12): la única
 * implementación hoy es TemplateMessageGenerator — sin proveedor de IA
 * real conectado todavía, porque no hace falta pagar por una API externa
 * para el MVP (mismo principio anti-sobreingeniería de toda la Fase 0).
 * Cuando exista un proveedor real (Anthropic, OpenAI, etc.), se agrega un
 * adapter nuevo que implemente MessageGenerator — NotificationService no
 * cambia una línea.
 */
package com.gymflow.ai;
