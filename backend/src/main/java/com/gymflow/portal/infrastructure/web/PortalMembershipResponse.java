package com.gymflow.portal.infrastructure.web;

import com.gymflow.membership.domain.Membership;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A diferencia de MembershipResponse (la vista del admin), acá va el
 * nombre del plan en vez de su UUID — un MEMBER no tiene acceso a
 * GET /api/v1/plans para resolverlo del lado del cliente, así que lo
 * resuelve el propio módulo portal usando la relación de dominio que
 * Membership ya expone (membership.getPlan()), sin depender de
 * PlanRepository ni de PlanService.
 */
public record PortalMembershipResponse(UUID id, String planName, LocalDate startDate, LocalDate endDate, String status) {

    public static PortalMembershipResponse from(Membership membership) {
        return new PortalMembershipResponse(
                membership.getId(),
                membership.getPlan().getName(),
                membership.getStartDate(),
                membership.getEndDate(),
                membership.getEffectiveStatus().name());
    }
}
