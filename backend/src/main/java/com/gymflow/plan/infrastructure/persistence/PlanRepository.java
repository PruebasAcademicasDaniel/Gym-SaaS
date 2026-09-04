package com.gymflow.plan.infrastructure.persistence;

import com.gymflow.plan.domain.Plan;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanRepository extends JpaRepository<Plan, UUID> {
}
