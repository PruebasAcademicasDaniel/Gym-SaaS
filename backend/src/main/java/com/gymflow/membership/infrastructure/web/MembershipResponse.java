package com.gymflow.membership.infrastructure.web;

import com.gymflow.membership.domain.Membership;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record MembershipResponse(
        UUID id, UUID memberId, UUID planId, LocalDate startDate, LocalDate endDate, String status, Instant createdAt) {

    public static MembershipResponse from(Membership membership) {
        return new MembershipResponse(
                membership.getId(),
                membership.getMember().getId(),
                membership.getPlan().getId(),
                membership.getStartDate(),
                membership.getEndDate(),
                membership.getEffectiveStatus().name(),
                membership.getCreatedAt());
    }
}
