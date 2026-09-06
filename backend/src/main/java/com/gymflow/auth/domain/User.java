package com.gymflow.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Identidad con la que se inicia sesión. gymId es null solo para
 * SUPER_ADMIN (alcance de plataforma). Tiene FK a gym desde la Fase 5 —
 * ver V3__create_gym_table.sql (V1 la dejó pendiente porque gym no existía).
 */
@Entity
@Table(name = "app_user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(name = "gym_id")
    private UUID gymId;

    /** Solo lo usa role MEMBER — el socio (member) al que este login representa. Ver validateRoleAssignment. */
    @Column(name = "member_id")
    private UUID memberId;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected User() {
        // JPA
    }

    public User(String email, String passwordHash, Role role, UUID gymId) {
        this(email, passwordHash, role, gymId, null);
    }

    /** Fase 13: constructor para logins de socio (role MEMBER), que necesitan memberId además de gymId. */
    public User(String email, String passwordHash, Role role, UUID gymId, UUID memberId) {
        validateRoleAssignment(role, gymId, memberId);
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.gymId = gymId;
        this.memberId = memberId;
        this.enabled = true;
    }

    /** Reconstrucción completa (tests, mapeos) — la creación normal usa los constructores de arriba, que dejan que la base asigne el id. */
    public User(UUID id, String email, String passwordHash, Role role, UUID gymId, UUID memberId, boolean enabled, Instant createdAt) {
        validateRoleAssignment(role, gymId, memberId);
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.gymId = gymId;
        this.memberId = memberId;
        this.enabled = enabled;
        this.createdAt = createdAt;
    }

    /**
     * Dos invariantes de dominio en un solo lugar:
     * (1) Fase 4 — todo rol salvo SUPER_ADMIN necesita un gymId. Sin esto,
     * un usuario mal creado con gymId null terminaría con acceso sin
     * restricción (ver GymTenantIdentifierResolver — null significa "sin
     * filtro").
     * (2) Fase 13 — memberId es obligatorio para MEMBER (un login de socio
     * sin socio asociado no tiene sentido) y prohibido para cualquier otro
     * rol (GYM_ADMIN/TRAINER/SUPER_ADMIN no representan a un socio).
     */
    private static void validateRoleAssignment(Role role, UUID gymId, UUID memberId) {
        boolean requiresGym = role != Role.SUPER_ADMIN;
        if (requiresGym && gymId == null) {
            throw new IllegalArgumentException("El rol " + role + " requiere un gymId.");
        }
        if (!requiresGym && gymId != null) {
            throw new IllegalArgumentException("SUPER_ADMIN no puede tener gymId — es de alcance de plataforma.");
        }
        boolean requiresMember = role == Role.MEMBER;
        if (requiresMember && memberId == null) {
            throw new IllegalArgumentException("El rol MEMBER requiere un memberId (el socio al que representa este login).");
        }
        if (!requiresMember && memberId != null) {
            throw new IllegalArgumentException("Solo el rol MEMBER puede tener memberId.");
        }
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

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public UUID getGymId() {
        return gymId;
    }

    public UUID getMemberId() {
        return memberId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void disable() {
        this.enabled = false;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
