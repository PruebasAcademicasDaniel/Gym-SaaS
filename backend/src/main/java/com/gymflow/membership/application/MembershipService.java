package com.gymflow.membership.application;

import com.gymflow.member.application.MemberService;
import com.gymflow.member.domain.Member;
import com.gymflow.membership.domain.Membership;
import com.gymflow.membership.domain.MembershipStatus;
import com.gymflow.membership.infrastructure.persistence.MembershipRepository;
import com.gymflow.plan.application.PlanService;
import com.gymflow.plan.domain.Plan;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Depende de MemberService y PlanService (capa de aplicación de esos
 * módulos), nunca de sus repositorios — así que si alguien manda un
 * memberId o planId de otro gimnasio, memberService.getById()/
 * planService.getById() ya lo rechazan solos (@TenantId los hace
 * invisibles) antes de que este servicio tenga que pensar en tenants.
 */
@Service
public class MembershipService {

    private final MembershipRepository membershipRepository;
    private final MemberService memberService;
    private final PlanService planService;

    public MembershipService(MembershipRepository membershipRepository, MemberService memberService, PlanService planService) {
        this.membershipRepository = membershipRepository;
        this.memberService = memberService;
        this.planService = planService;
    }

    @Transactional
    public Membership create(UUID memberId, UUID planId) {
        Member member = memberService.getById(memberId);
        Plan plan = planService.getById(planId);
        if (!plan.isActive()) {
            throw new IllegalArgumentException("El plan '" + plan.getName() + "' no está activo.");
        }
        return membershipRepository.save(new Membership(member, plan, LocalDate.now()));
    }

    public List<Membership> listByMember(UUID memberId) {
        memberService.getById(memberId); // 404 temprano y consistente si el socio no existe o es de otro gimnasio
        return membershipRepository.findByMemberIdOrderByStartDateDesc(memberId);
    }

    /** Puerta pública para otros módulos (payment) — igual que MemberService/PlanService.getById(). */
    public Membership getById(UUID id) {
        return membershipRepository.findById(id).orElseThrow(() -> new MembershipNotFoundException(id));
    }

    @Transactional
    public void cancel(UUID id) {
        getById(id).cancel();
    }

    /** Socios distintos con una membresía vigente hoy — puerta pública para dashboard. */
    public long countActiveMembers() {
        return membershipRepository.countDistinctMembersWithStatusAndNotExpired(MembershipStatus.ACTIVE, LocalDate.now());
    }

    /** Membresías vigentes que vencen dentro de la ventana [hoy, hoy + días] — puerta pública para dashboard. */
    public long countExpiringWithinDays(int days) {
        LocalDate today = LocalDate.now();
        return membershipRepository.countByStatusAndEndDateBetween(MembershipStatus.ACTIVE, today, today.plusDays(days));
    }

    /** Mismo criterio que countExpiringWithinDays, pero devolviendo las membresías — puerta pública para notification (Fase 12). */
    public List<Membership> listExpiringWithinDays(int days) {
        LocalDate today = LocalDate.now();
        return membershipRepository.findByStatusAndEndDateBetween(MembershipStatus.ACTIVE, today, today.plusDays(days));
    }

    /** Todo el universo de membresías vigentes hoy, sin ventana superior — puerta pública para risk (Fase 14). */
    public List<Membership> listActiveMemberships() {
        return membershipRepository.findByStatusAndEndDateGreaterThanEqual(MembershipStatus.ACTIVE, LocalDate.now());
    }
}
