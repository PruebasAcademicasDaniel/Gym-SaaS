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
 * SUPER_ADMIN (alcance de plataforma). No tiene FK a gym todavía porque
 * esa tabla no existe hasta la Fase 5 — ver V1__create_auth_tables.sql.
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
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.gymId = gymId;
        this.enabled = true;
    }

    /** Reconstrucción completa (tests, mapeos) — la creación normal usa el constructor de arriba, que deja que la base asigne el id. */
    public User(UUID id, String email, String passwordHash, Role role, UUID gymId, boolean enabled, Instant createdAt) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.gymId = gymId;
        this.enabled = enabled;
        this.createdAt = createdAt;
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

    public Instant getCreatedAt() {
        return createdAt;
    }
}
