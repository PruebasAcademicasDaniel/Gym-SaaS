package com.gymflow.attendance.infrastructure.persistence;

import com.gymflow.attendance.domain.Attendance;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceRepository extends JpaRepository<Attendance, UUID> {

    List<Attendance> findByMemberIdOrderByCheckedInAtDesc(UUID memberId);
}
