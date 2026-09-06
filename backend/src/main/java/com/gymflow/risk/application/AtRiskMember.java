package com.gymflow.risk.application;

import com.gymflow.member.domain.Member;
import java.time.LocalDate;
import java.util.UUID;

/**
 * lastActivity es el último check-in conocido, o la fecha de inicio de su
 * membresía actual si nunca asistió. membershipId viaja acá (no solo el
 * Member) porque notification (Fase 14) necesita imputar la alerta a una
 * membresía concreta, igual que ya hace con MEMBERSHIP_EXPIRING_SOON.
 */
public record AtRiskMember(Member member, UUID membershipId, LocalDate lastActivity) {
}
