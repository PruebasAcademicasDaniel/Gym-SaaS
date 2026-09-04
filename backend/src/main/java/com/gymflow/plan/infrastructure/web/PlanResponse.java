package com.gymflow.plan.infrastructure.web;

import com.gymflow.plan.domain.Plan;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PlanResponse(
        UUID id, String name, String description, BigDecimal price, int durationDays, boolean active, Instant createdAt) {

    public static PlanResponse from(Plan plan) {
        return new PlanResponse(
                plan.getId(), plan.getName(), plan.getDescription(), plan.getPrice(), plan.getDurationDays(), plan.isActive(),
                plan.getCreatedAt());
    }
}
