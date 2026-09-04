package com.gymflow.gym.infrastructure.web;

import com.gymflow.gym.application.GymService;
import com.gymflow.gym.domain.Gym;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/gyms")
public class GymController {

    private final GymService gymService;

    public GymController(GymService gymService) {
        this.gymService = gymService;
    }

    /** Alta de un gimnasio nuevo — solo la plataforma (SUPER_ADMIN) da de alta tenants. */
    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<GymResponse> create(@Valid @RequestBody CreateGymRequest request) {
        Gym gym = gymService.create(request.name(), request.slug());
        return ResponseEntity.status(HttpStatus.CREATED).body(GymResponse.from(gym));
    }

    /** SUPER_ADMIN ve cualquier gimnasio; GYM_ADMIN, solo el propio. */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or (hasRole('GYM_ADMIN') and #id == authentication.principal.gymId())")
    public GymResponse getById(@PathVariable UUID id) {
        return GymResponse.from(gymService.getById(id));
    }
}
