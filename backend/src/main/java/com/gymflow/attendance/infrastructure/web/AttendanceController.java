package com.gymflow.attendance.infrastructure.web;

import com.gymflow.attendance.application.AttendanceService;
import com.gymflow.attendance.domain.Attendance;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Primer endpoint donde TRAINER escribe, no solo lee — la matriz de
 * permisos de la Fase 0 le da "Registrar / lectura" en Asistencia, a
 * diferencia de Socios/Planes/Membresías donde es de solo lectura.
 */
@RestController
public class AttendanceController {

    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @PostMapping("/api/v1/attendance")
    @PreAuthorize("hasRole('GYM_ADMIN') or hasRole('TRAINER')")
    public ResponseEntity<AttendanceResponse> checkIn(@Valid @RequestBody CheckInRequest request) {
        Attendance attendance = attendanceService.checkIn(request.memberId());
        return ResponseEntity.status(HttpStatus.CREATED).body(AttendanceResponse.from(attendance));
    }

    @GetMapping("/api/v1/members/{memberId}/attendance")
    @PreAuthorize("hasRole('GYM_ADMIN') or hasRole('TRAINER')")
    public List<AttendanceResponse> listByMember(@PathVariable UUID memberId) {
        return attendanceService.listByMember(memberId).stream().map(AttendanceResponse::from).toList();
    }
}
