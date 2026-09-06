-- Fase 14: la Fase 0 (sección 8, funcionalidades del MVP) pidió notificar
-- tanto vencimiento próximo como cliente en riesgo; la Fase 12 solo pudo
-- construir la primera porque el motor de riesgo (Fase 14) todavía no
-- existía. Ahora que existe, se habilita el segundo tipo reutilizando la
-- misma tabla/índice de idempotencia (membership_id, type) — un socio en
-- riesgo se notifica una sola vez por membresía, igual que un vencimiento.
ALTER TABLE notification DROP CONSTRAINT notification_type_check;
ALTER TABLE notification ADD CONSTRAINT notification_type_check CHECK (type IN ('MEMBERSHIP_EXPIRING_SOON', 'MEMBER_AT_RISK'));
