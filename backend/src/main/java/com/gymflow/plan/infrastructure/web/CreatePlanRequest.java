package com.gymflow.plan.infrastructure.web;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record CreatePlanRequest(
        @NotBlank String name,
        String description,
        @NotNull @Positive BigDecimal price,
        @Min(1) int durationDays) {
}
