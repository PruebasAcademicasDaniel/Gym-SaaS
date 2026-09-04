/**
 * Vacío a propósito desde la Fase 5: la gestión de usuarios internos
 * (alta, listado, baja de GYM_ADMIN/TRAINER) terminó viviendo en
 * com.gymflow.auth.application.UserManagementService, no acá. User y su
 * repositorio ya vivían en auth desde la Fase 3 por el login; separar el
 * CRUD en un módulo aparte hubiera partido un mismo agregado en dos
 * módulos — peor que la separación conceptual que sugería la Fase 0.
 */
package com.gymflow.user;
