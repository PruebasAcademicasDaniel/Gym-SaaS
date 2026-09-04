package com.gymflow.member.infrastructure.web;

import com.gymflow.auth.infrastructure.security.AuthenticatedPrincipal;
import com.gymflow.member.application.MemberService;
import com.gymflow.member.domain.Member;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sin chequeos de gymId acá — a diferencia de UserController, no hacen
 * falta: MemberService ya opera sobre entidades acotadas al tenant actual
 * por Hibernate. Lo único que varía por rol es qué se puede hacer
 * (GYM_ADMIN escribe, TRAINER solo lee — sección 6 de la Fase 0).
 */
@RestController
@RequestMapping("/api/v1/members")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @PostMapping
    @PreAuthorize("hasRole('GYM_ADMIN')")
    public ResponseEntity<MemberResponse> create(
            @Valid @RequestBody CreateMemberRequest request,
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        Member member = memberService.create(principal.userId(), request.firstName(), request.lastName(), request.email(), request.phone());
        return ResponseEntity.status(HttpStatus.CREATED).body(MemberResponse.from(member));
    }

    @GetMapping
    @PreAuthorize("hasRole('GYM_ADMIN') or hasRole('TRAINER')")
    public List<MemberResponse> list() {
        return memberService.list().stream().map(MemberResponse::from).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('GYM_ADMIN') or hasRole('TRAINER')")
    public MemberResponse getById(@PathVariable UUID id) {
        return MemberResponse.from(memberService.getById(id));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('GYM_ADMIN')")
    public MemberResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateMemberRequest request) {
        Member member = memberService.update(id, request.firstName(), request.lastName(), request.email(), request.phone());
        return MemberResponse.from(member);
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('GYM_ADMIN')")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        memberService.deactivate(principal.userId(), id);
        return ResponseEntity.noContent().build();
    }
}
