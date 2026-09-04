package com.gymflow.payment.infrastructure.web;

import com.gymflow.auth.infrastructure.security.AuthenticatedPrincipal;
import com.gymflow.payment.application.PaymentService;
import com.gymflow.payment.domain.Payment;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Sin TRAINER acá — a diferencia de member/plan, la matriz de permisos de la Fase 0 no le da acceso a Pagos. */
@RestController
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/api/v1/payments")
    @PreAuthorize("hasRole('GYM_ADMIN')")
    public ResponseEntity<PaymentResponse> create(
            @Valid @RequestBody CreatePaymentRequest request,
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        Payment payment = paymentService.register(principal.userId(), request.membershipId(), request.amount(), request.method());
        return ResponseEntity.status(HttpStatus.CREATED).body(PaymentResponse.from(payment));
    }

    @GetMapping("/api/v1/memberships/{membershipId}/payments")
    @PreAuthorize("hasRole('GYM_ADMIN')")
    public List<PaymentResponse> listByMembership(@PathVariable UUID membershipId) {
        return paymentService.listByMembership(membershipId).stream().map(PaymentResponse::from).toList();
    }

    @GetMapping("/api/v1/payments/{id}")
    @PreAuthorize("hasRole('GYM_ADMIN')")
    public PaymentResponse getById(@PathVariable UUID id) {
        return PaymentResponse.from(paymentService.getById(id));
    }
}
