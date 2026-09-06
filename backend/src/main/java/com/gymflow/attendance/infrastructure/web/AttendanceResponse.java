package com.gymflow.attendance.infrastructure.web;

import com.gymflow.attendance.domain.Attendance;
import java.time.Instant;
import java.util.UUID;

public record AttendanceResponse(UUID id, UUID memberId, Instant checkedInAt) {

    public static AttendanceResponse from(Attendance attendance) {
        return new AttendanceResponse(attendance.getId(), attendance.getMember().getId(), attendance.getCheckedInAt());
    }
}
