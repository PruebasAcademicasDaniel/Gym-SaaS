/**
 * Portal de autoservicio para el rol MEMBER (Fase 13) — el "client-portal"
 * del documento de arquitectura. Sin entidad ni tabla propia, igual que
 * dashboard: solo compone datos de otros módulos (member, membership,
 * payment, attendance) a través de su capa de aplicación pública, siempre
 * acotados al memberId del propio socio autenticado — nunca a un id que
 * venga en la URL o el body, así un MEMBER estructuralmente no puede pedir
 * los datos de otro socio ni por error de otro cliente. Todo de solo
 * lectura: sin auto-baja, sin edición de perfil, sin contratar un plan
 * desde acá — eso queda post-MVP si el negocio lo pide.
 */
package com.gymflow.portal;
