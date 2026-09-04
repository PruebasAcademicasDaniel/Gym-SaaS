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

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected User() {
        // JPA
    }

    public User(String email, String passwordHash, Role role, UUID gymId) {
        validateGymAssignment(role, gymId);
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.gymId = gymId;
        this.enabled = true;
    }

    /** Reconstrucción completa (tests, mapeos) — la creación normal usa el constructor de arriba, que deja que la base asigne el id. */
    public User(UUID id, String email, String passwordHash, Role role, UUID gymId, boolean enabled, Instant createdAt) {
        validateGymAssignment(role, gymId);
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.gymId = gymId;
        this.enabled = enabled;
        this.createdAt = createdAt;
    }

    /**
     * Invariante de tenant clave para toda la Fase 4: todo rol salvo
     * SUPER_ADMIN necesita un gymId. Sin esto, un usuario mal creado con
     * gymId null terminaría con acceso sin restricción (ver
     * GymTenantIdentifierResolver — null significa "sin filtro").
     */
    private static void validateGymAssignment(Role role, UUID gymId) {
        boolean requiresGym = role != Role.SUPER_ADMIN;
        if (requiresGym && gymId == null) {
            throw new IllegalArgumentException("El rol " + role + " requiere un gymId.");
        }
        if (!requiresGym && gymId != null) {
            throw new IllegalArgumentException("SUPER_ADMIN no puede tener gymId — es de alcance de plataforma.");
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
