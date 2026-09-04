package com.gymflow.payment.infrastructure.web;

import com.gymflow.payment.domain.Payment;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PaymentResponse(
        UUID id, UUID membershipId, BigDecimal amount, String method, LocalDate paymentDate, Instant createdAt) {

    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getMembership().getId(),
                payment.getAmount(),
                payment.getMethod().name(),
                payment.getPaymentDate(),
                payment.getCreatedAt());
    }
}
