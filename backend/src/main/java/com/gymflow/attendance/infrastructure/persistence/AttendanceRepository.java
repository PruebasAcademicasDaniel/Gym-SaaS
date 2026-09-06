package com.gymflow.attendance.infrastructure.persistence;

import com.gymflow.attendance.domain.Attendance;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceRepository extends JpaRepository<Attendance, UUID> {

    List<Attendance> findByMemberIdOrderByCheckedInAtDesc(UUID memberId);

    /** Solo el check-in más reciente de un socio — para el módulo risk (Fase 14), que no necesita el historial completo. */
    Optional<Attendance> findTopByMemberIdOrderByCheckedInAtDesc(UUID memberId);
}
