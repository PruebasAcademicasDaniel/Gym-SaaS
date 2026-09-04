package com.gymflow.membership.infrastructure.web;

import com.gymflow.membership.application.MembershipService;
import com.gymflow.membership.domain.Membership;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MembershipController {

    private final MembershipService membershipService;

    public MembershipController(MembershipService membershipService) {
        this.membershipService = membershipService;
    }

    @PostMapping("/api/v1/memberships")
    @PreAuthorize("hasRole('GYM_ADMIN')")
    public ResponseEntity<MembershipResponse> create(@Valid @RequestBody CreateMembershipRequest request) {
        Membership membership = membershipService.create(request.memberId(), request.planId());
        return ResponseEntity.status(HttpStatus.CREATED).body(MembershipResponse.from(membership));
    }

    /** Histórico completo del socio, no solo la activa — sección 5 del documento de arquitectura. */
    @GetMapping("/api/v1/members/{memberId}/memberships")
    @PreAuthorize("hasRole('GYM_ADMIN') or hasRole('TRAINER')")
    public List<MembershipResponse> listByMember(@PathVariable UUID memberId) {
        return membershipService.listByMember(memberId).stream().map(MembershipResponse::from).toList();
    }

    @PatchMapping("/api/v1/memberships/{id}/cancel")
    @PreAuthorize("hasRole('GYM_ADMIN')")
    public ResponseEntity<Void> cancel(@PathVariable UUID id) {
        membershipService.cancel(id);
        return ResponseEntity.noContent().build();
    }
}
