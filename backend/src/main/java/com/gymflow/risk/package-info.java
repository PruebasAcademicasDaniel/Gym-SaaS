/**
 * Motor determinístico de detección de clientes en riesgo de abandono
 * (Fase 14) — sin IA ni scoring probabilístico, eso es la Fase 15. Sin
 * entidad ni tabla propia, mismo patrón agregador que dashboard (Fase 10)
 * y portal (Fase 13): compone datos de member/membership/attendance a
 * través de su capa de aplicación pública. La regla en sí vive en
 * risk.domain.RiskPolicy — una función pura, sin Spring ni JPA, tal como
 * pide la Fase 0 para las reglas de negocio de este módulo.
 */
package com.gymflow.risk;
