package com.gymflow.membership.application;

import com.gymflow.member.application.MemberService;
import com.gymflow.member.domain.Member;
import com.gymflow.membership.domain.Membership;
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
}
