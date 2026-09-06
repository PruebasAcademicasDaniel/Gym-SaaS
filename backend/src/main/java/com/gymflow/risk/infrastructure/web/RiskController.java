package com.gymflow.risk.infrastructure.web;

import com.gymflow.risk.application.RiskService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** GYM_ADMIN únicamente — mismo criterio de permisos que Dashboard (Fase 10): TRAINER no tiene acceso a vistas agregadas de negocio. */
@RestController
@RequestMapping("/api/v1/risk")
public class RiskController {

    private final RiskService riskService;

    public RiskController(RiskService riskService) {
        this.riskService = riskService;
    }

    @GetMapping("/members")
    @PreAuthorize("hasRole('GYM_ADMIN')")
    public List<RiskMemberResponse> listAtRiskMembers() {
        return riskService.listAtRiskMembers().stream().map(RiskMemberResponse::from).toList();
    }
}
