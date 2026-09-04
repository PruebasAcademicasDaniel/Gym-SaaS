package com.gymflow.plan.domain;

import com.gymflow.shared.tenant.AbstractTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "plan")
public class Plan extends AbstractTenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "duration_days", nullable = false)
    private int durationDays;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Plan() {
        // JPA
    }

    public Plan(String name, String description, BigDecimal price, int durationDays) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.durationDays = durationDays;
        this.active = true;
    }

    @PrePersist
    void onPersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public void update(String name, String description, BigDecimal price, int durationDays) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.durationDays = durationDays;
    }

    public void deactivate() {
        this.active = false;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public int getDurationDays() {
        return durationDays;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
