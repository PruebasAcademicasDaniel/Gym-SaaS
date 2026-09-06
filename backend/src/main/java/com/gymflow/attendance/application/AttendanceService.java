package com.gymflow.attendance.application;

import com.gymflow.attendance.domain.Attendance;
import com.gymflow.attendance.infrastructure.persistence.AttendanceRepository;
import com.gymflow.member.application.MemberService;
import com.gymflow.member.domain.Member;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Depende de MemberService, no de MemberRepository — mismo patrón que
 * MembershipService/PaymentService (Fases 7 y 8). Un memberId de otro
 * gimnasio se rechaza solo con 404, sin que este servicio piense en
 * tenants.
 */
@Service
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final MemberService memberService;

    public AttendanceService(AttendanceRepository attendanceRepository, MemberService memberService) {
        this.attendanceRepository = attendanceRepository;
        this.memberService = memberService;
    }

    @Transactional
    public Attendance checkIn(UUID memberId) {
        Member member = memberService.getById(memberId);
        return attendanceRepository.save(new Attendance(member, Instant.now()));
    }

    public List<Attendance> listByMember(UUID memberId) {
        memberService.getById(memberId); // 404 temprano si el socio no existe o es de otro gimnasio
        return attendanceRepository.findByMemberIdOrderByCheckedInAtDesc(memberId);
    }
}
