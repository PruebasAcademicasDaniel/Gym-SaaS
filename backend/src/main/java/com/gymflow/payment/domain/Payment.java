package com.gymflow.payment.domain;

import com.gymflow.membership.domain.Membership;
import com.gymflow.shared.tenant.AbstractTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Sin update() ni setters más allá del constructor — a propósito. "Registro
 * manual de pagos" (Fase 0) significa eso: se registra, no se edita. Una
 * corrección real necesitaría su propio mecanismo de ajuste/reverso, que
 * no es parte del MVP.
 */
@Entity
@Table(name = "payment")
public class Payment extends AbstractTenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "membership_id", nullable = false)
    private Membership membership;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod method;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Payment() {
        // JPA
    }

    public Payment(Membership membership, BigDecimal amount, PaymentMethod method, LocalDate paymentDate) {
        this.membership = membership;
        this.amount = amount;
        this.method = method;
        this.paymentDate = paymentDate;
    }

    @PrePersist
    void onPersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public Membership getMembership() {
        return membership;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
