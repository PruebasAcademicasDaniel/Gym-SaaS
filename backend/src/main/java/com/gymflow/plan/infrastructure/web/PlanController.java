package com.gymflow.plan.infrastructure.web;

import com.gymflow.plan.application.PlanService;
import com.gymflow.plan.domain.Plan;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/plans")
public class PlanController {

    private final PlanService planService;

    public PlanController(PlanService planService) {
        this.planService = planService;
    }

    @PostMapping
    @PreAuthorize("hasRole('GYM_ADMIN')")
    public ResponseEntity<PlanResponse> create(@Valid @RequestBody CreatePlanRequest request) {
        Plan plan = planService.create(request.name(), request.description(), request.price(), request.durationDays());
        return ResponseEntity.status(HttpStatus.CREATED).body(PlanResponse.from(plan));
    }

    @GetMapping
    @PreAuthorize("hasRole('GYM_ADMIN') or hasRole('TRAINER')")
    public List<PlanResponse> list() {
        return planService.list().stream().map(PlanResponse::from).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('GYM_ADMIN') or hasRole('TRAINER')")
    public PlanResponse getById(@PathVariable UUID id) {
        return PlanResponse.from(planService.getById(id));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('GYM_ADMIN')")
    public PlanResponse update(@PathVariable UUID id, @Valid @RequestBody UpdatePlanRequest request) {
        Plan plan = planService.update(id, request.name(), request.description(), request.price(), request.durationDays());
        return PlanResponse.from(plan);
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('GYM_ADMIN')")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        planService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
