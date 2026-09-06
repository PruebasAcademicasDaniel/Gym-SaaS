package com.gymflow.risk.application;

import com.gymflow.attendance.application.AttendanceService;
import com.gymflow.member.application.MemberService;
import com.gymflow.membership.application.MembershipService;
import com.gymflow.membership.domain.Membership;
import com.gymflow.risk.domain.RiskPolicy;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Agregador puro, sin entidad ni repositorio propios — mismo patrón que
 * DashboardService (Fase 10) y PortalService (Fase 13). Depende de
 * MembershipService/AttendanceService/MemberService (capa de aplicación),
 * nunca de sus repositorios.
 *
 * Solo se llama a memberService.getById() para los socios que YA pasaron
 * el filtro de riesgo, no para todo el universo de membresías activas —
 * evita traer el Member completo (con sus columnas propias) para la
 * mayoría que no está en riesgo. Membership.getMember().getId() alcanza
 * para armar el mapa de candidatos porque un proxy LAZY conoce su propio
 * id sin inicializarse (ver gymflow_infra_gotchas #13 sobre por qué NO se
 * puede llamar ningún otro getter del proxy acá).
 */
@Service
public class RiskService {

    private final MembershipService membershipService;
    private final AttendanceService attendanceService;
    private final MemberService memberService;

    public RiskService(MembershipService membershipService, AttendanceService attendanceService, MemberService memberService) {
        this.membershipService = membershipService;
        this.attendanceService = attendanceService;
        this.memberService = memberService;
    }

    public List<AtRiskMember> listAtRiskMembers() {
        LocalDate today = LocalDate.now();

        // Un socio puede (en teoría) tener más de una membresía ACTIVE a la vez; se toma la de inicio más reciente como línea de base.
        Map<UUID, Membership> latestActiveByMemberId = new LinkedHashMap<>();
        for (Membership membership : membershipService.listActiveMemberships()) {
            UUID memberId = membership.getMember().getId();
            latestActiveByMemberId.merge(memberId, membership,
                    (current, candidate) -> candidate.getStartDate().isAfter(current.getStartDate()) ? candidate : current);
        }

        List<AtRiskMember> atRisk = new ArrayList<>();
        for (Membership membership : latestActiveByMemberId.values()) {
            UUID memberId = membership.getMember().getId();
            LocalDate lastActivity = attendanceService.getLastCheckInDate(memberId).orElse(membership.getStartDate());
            if (RiskPolicy.isAtRisk(lastActivity, today)) {
                atRisk.add(new AtRiskMember(memberService.getById(memberId), membership.getId(), lastActivity));
            }
        }
        return atRisk;
    }

    /** Puerta pública para dashboard (Fase 10 dejó este número deliberadamente afuera hasta que este motor existiera). */
    public long countAtRiskMembers() {
        return listAtRiskMembers().size();
    }
}
