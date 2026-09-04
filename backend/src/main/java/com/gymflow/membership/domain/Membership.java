package com.gymflow.membership.domain;

import com.gymflow.member.domain.Member;
import com.gymflow.plan.domain.Plan;
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
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Referencia a Member y Plan (entidades de otros módulos) como relaciones
 * de dominio normales — eso no es "importar infraestructura de otro
 * módulo" (la regla de la Fase 0, sección 9), es exactamente cómo se
 * modelan relaciones dentro de un monolito modular. Lo que sí evita:
 * MembershipService nunca llama a MemberRepository/PlanRepository
 * directamente, solo a MemberService.getById()/PlanService.getById() —
 * eso sí sería cruzar la infraestructura de otro módulo.
 */
@Entity
@Table(name = "membership")
public class Membership extends AbstractTenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MembershipStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Membership() {
        // JPA
    }

    public Membership(Member member, Plan plan, LocalDate startDate) {
        this.member = member;
        this.plan = plan;
        this.startDate = startDate;
        this.endDate = startDate.plusDays(plan.getDurationDays());
        this.status = MembershipStatus.ACTIVE;
    }

    @PrePersist
    void onPersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public void cancel() {
        this.status = MembershipStatus.CANCELLED;
    }

    /** El estado que importa mostrar: CANCELLED gana siempre; si no, vencida o activa según la fecha. */
    public MembershipStatus getEffectiveStatus() {
        if (status == MembershipStatus.CANCELLED) {
            return MembershipStatus.CANCELLED;
        }
        return endDate.isBefore(LocalDate.now()) ? MembershipStatus.EXPIRED : MembershipStatus.ACTIVE;
    }

    public UUID getId() {
        return id;
    }

    public Member getMember() {
        return member;
    }

    public Plan getPlan() {
        return plan;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
