package com.gymflow.risk.infrastructure.web;

import com.gymflow.member.domain.Member;
import com.gymflow.risk.application.AtRiskMember;
import java.time.LocalDate;
import java.util.UUID;

public record RiskMemberResponse(
        UUID id, String firstName, String lastName, String email, String phone, LocalDate lastActivity) {

    public static RiskMemberResponse from(AtRiskMember atRisk) {
        Member member = atRisk.member();
        return new RiskMemberResponse(
                member.getId(), member.getFirstName(), member.getLastName(), member.getEmail(), member.getPhone(),
                atRisk.lastActivity());
    }
}
