package com.gymflow.member.application;

import com.gymflow.audit.application.AuditService;
import com.gymflow.audit.domain.AuditAction;
import com.gymflow.member.domain.Member;
import com.gymflow.member.infrastructure.persistence.MemberRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CRUD de socios. A propósito no recibe ni valida gymId en ningún método
 * — Member extiende AbstractTenantEntity, así que el tenant lo resuelve
 * Hibernate solo (ver Fase 4). El actor sí se pasa explícito, para
 * auditoría (altas y bajas de socio son parte de la auditoría mínima del
 * MVP — Fase 0, sección 12).
 */
@Service
public class MemberService {

    private final MemberRepository memberRepository;
    private final AuditService auditService;

    public MemberService(MemberRepository memberRepository, AuditService auditService) {
        this.memberRepository = memberRepository;
        this.auditService = auditService;
    }

    @Transactional
    public Member create(UUID actorUserId, String firstName, String lastName, String email, String phone) {
        Member member = memberRepository.save(new Member(firstName, lastName, email, phone));
        auditService.record(member.getGymId(), actorUserId, AuditAction.MEMBER_CREATED, member.getId().toString());
        return member;
    }

    public Member getById(UUID id) {
        return memberRepository.findById(id).orElseThrow(() -> new MemberNotFoundException(id));
    }

    public List<Member> list() {
        return memberRepository.findAll();
    }

    @Transactional
    public Member update(UUID id, String firstName, String lastName, String email, String phone) {
        Member member = getById(id);
        member.updateContactInfo(firstName, lastName, email, phone);
        return member;
    }

    @Transactional
    public void deactivate(UUID actorUserId, UUID id) {
        Member member = getById(id);
        member.deactivate();
        auditService.record(member.getGymId(), actorUserId, AuditAction.MEMBER_DEACTIVATED, member.getId().toString());
    }
}
