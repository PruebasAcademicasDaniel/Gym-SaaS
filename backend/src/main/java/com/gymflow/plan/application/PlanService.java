package com.gymflow.plan.application;

import com.gymflow.plan.domain.Plan;
import com.gymflow.plan.infrastructure.persistence.PlanRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * getById() es la puerta pública que usa el módulo membership para
 * resolver un plan — nunca importa PlanRepository directamente (regla de
 * la Fase 0, sección 9: entre módulos, solo por la capa de aplicación).
 */
@Service
public class PlanService {

    private final PlanRepository planRepository;

    public PlanService(PlanRepository planRepository) {
        this.planRepository = planRepository;
    }

    @Transactional
    public Plan create(String name, String description, BigDecimal price, int durationDays) {
        return planRepository.save(new Plan(name, description, price, durationDays));
    }

    public Plan getById(UUID id) {
        return planRepository.findById(id).orElseThrow(() -> new PlanNotFoundException(id));
    }

    public List<Plan> list() {
        return planRepository.findAll();
    }

    @Transactional
    public Plan update(UUID id, String name, String description, BigDecimal price, int durationDays) {
        Plan plan = getById(id);
        plan.update(name, description, price, durationDays);
        return plan;
    }

    @Transactional
    public void deactivate(UUID id) {
        getById(id).deactivate();
    }
}
