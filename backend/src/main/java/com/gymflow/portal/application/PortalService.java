package com.gymflow.portal.application;

import com.gymflow.attendance.application.AttendanceService;
import com.gymflow.attendance.domain.Attendance;
import com.gymflow.member.application.MemberService;
import com.gymflow.member.domain.Member;
import com.gymflow.membership.application.MembershipService;
import com.gymflow.membership.domain.Membership;
import com.gymflow.payment.application.PaymentService;
import com.gymflow.payment.domain.Payment;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Puro agregador, sin entidad ni repositorio propios — mismo patrón que
 * DashboardService (Fase 10). Depende de MemberService/MembershipService/
 * PaymentService/AttendanceService (capa de aplicación), nunca de sus
 * repositorios. No hay método "listByMember" para pagos en PaymentService
 * porque a ese módulo no le hace falta — acá se compone recorriendo las
 * membresías del socio, que ya es el patrón que usa cada módulo para
 * apoyarse en los de al lado.
 */
@Service
public class PortalService {

    private final MemberService memberService;
    private final MembershipService membershipService;
    private final PaymentService paymentService;
    private final AttendanceService attendanceService;

    public PortalService(
            MemberService memberService,
            MembershipService membershipService,
            PaymentService paymentService,
            AttendanceService attendanceService) {
        this.memberService = memberService;
        this.membershipService = membershipService;
        this.paymentService = paymentService;
        this.attendanceService = attendanceService;
    }

    public Member getMyProfile(UUID memberId) {
        return memberService.getById(memberId);
    }

    public List<Membership> getMyMemberships(UUID memberId) {
        return membershipService.listByMember(memberId);
    }

    public List<Payment> getMyPayments(UUID memberId) {
        return membershipService.listByMember(memberId).stream()
                .flatMap(membership -> paymentService.listByMembership(membership.getId()).stream())
                .sorted(Comparator.comparing(Payment::getPaymentDate).reversed())
                .toList();
    }

    public List<Attendance> getMyAttendance(UUID memberId) {
        return attendanceService.listByMember(memberId);
    }
}
