package com.gymflow.payment.infrastructure.web;

import com.gymflow.payment.domain.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;

public record CreatePaymentRequest(
        @NotNull UUID membershipId,
        @NotNull @Positive BigDecimal amount,
        @NotNull PaymentMethod method) {
}
