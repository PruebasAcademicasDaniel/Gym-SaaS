package com.gymflow.dashboard.infrastructure.web;

import com.gymflow.dashboard.application.DashboardService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Solo GYM_ADMIN. La matriz de permisos de la Fase 0 le da a TRAINER una
 * vista "acotada a sus socios" — pero esa vista requiere el modelo de
 * asignación trainer↔member que la Fase 0 explícitamente dejó fuera del
 * MVP ("no crítica para el MVP"). Sin esa asignación, un dashboard
 * "acotado" para TRAINER no tiene con qué acotarse, así que queda diferido
 * junto con esa asignación en vez de construirse a medias.
 */
@RestController
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/api/v1/dashboard")
    @PreAuthorize("hasRole('GYM_ADMIN')")
    public DashboardResponse getSummary() {
        return DashboardResponse.from(dashboardService.getSummary());
    }
}
