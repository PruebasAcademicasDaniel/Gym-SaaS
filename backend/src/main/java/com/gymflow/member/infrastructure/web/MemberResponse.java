package com.gymflow.member.infrastructure.web;

import com.gymflow.member.domain.Member;
import java.time.Instant;
import java.util.UUID;

public record MemberResponse(
        UUID id, String firstName, String lastName, String email, String phone, boolean active, Instant createdAt) {

    public static MemberResponse from(Member member) {
        return new MemberResponse(
                member.getId(),
                member.getFirstName(),
                member.getLastName(),
                member.getEmail(),
                member.getPhone(),
                member.isActive(),
                member.getCreatedAt());
    }
}
