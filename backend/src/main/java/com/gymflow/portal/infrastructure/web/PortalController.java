package com.gymflow.portal.infrastructure.web;

import com.gymflow.attendance.infrastructure.web.AttendanceResponse;
import com.gymflow.auth.infrastructure.security.AuthenticatedPrincipal;
import com.gymflow.member.infrastructure.web.MemberResponse;
import com.gymflow.payment.infrastructure.web.PaymentResponse;
import com.gymflow.portal.application.PortalService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * A diferencia de todo otro controller del proyecto, ninguna ruta acá toma
 * un id por path/query: el memberId siempre sale de principal.memberId()
 * (el claim del JWT, firmado, no falsificable por el cliente) — así un
 * MEMBER no tiene ni forma de pedir los datos de otro socio, ni por error
 * de un cliente mal escrito ni por un ataque deliberado. Solo GET: el
 * portal es de solo lectura en el MVP.
 */
@RestController
@RequestMapping("/api/v1/portal")
public class PortalController {

    private final PortalService portalService;

    public PortalController(PortalService portalService) {
        this.portalService = portalService;
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('MEMBER')")
    public MemberResponse me(@AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return MemberResponse.from(portalService.getMyProfile(principal.memberId()));
    }

    @GetMapping("/memberships")
    @PreAuthorize("hasRole('MEMBER')")
    public List<PortalMembershipResponse> myMemberships(@AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return portalService.getMyMemberships(principal.memberId()).stream().map(PortalMembershipResponse::from).toList();
    }

    @GetMapping("/payments")
    @PreAuthorize("hasRole('MEMBER')")
    public List<PaymentResponse> myPayments(@AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return portalService.getMyPayments(principal.memberId()).stream().map(PaymentResponse::from).toList();
    }

    @GetMapping("/attendance")
    @PreAuthorize("hasRole('MEMBER')")
    public List<AttendanceResponse> myAttendance(@AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return portalService.getMyAttendance(principal.memberId()).stream().map(AttendanceResponse::from).toList();
    }
}
