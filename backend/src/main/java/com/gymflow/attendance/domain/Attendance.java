package com.gymflow.attendance.domain;

import com.gymflow.member.domain.Member;
import com.gymflow.shared.tenant.AbstractTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Un registro por check-in, sin deduplicar (ni por día). Fuente de datos
 * del motor de riesgo (Fase 14) — cualquier agregación ("días distintos
 * asistidos", "última visita") se calcula en la consulta que la necesite,
 * no acá. Sin update ni cancel: un check-in registrado no se corrige,
 * mismo criterio que Payment en la Fase 8.
 */
@Entity
@Table(name = "attendance")
public class Attendance extends AbstractTenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "checked_in_at", nullable = false)
    private Instant checkedInAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Attendance() {
        // JPA
    }

    public Attendance(Member member, Instant checkedInAt) {
        this.member = member;
        this.checkedInAt = checkedInAt;
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

    public Member getMember() {
        return member;
    }

    public Instant getCheckedInAt() {
        return checkedInAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
