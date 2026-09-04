package com.gymflow.member.infrastructure.persistence;

import com.gymflow.member.domain.Member;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Sin ningún método "byGymId" — a diferencia de UserRepository, acá no
 * hace falta: Member extiende AbstractTenantEntity, así que findAll() y
 * findById() ya vienen filtrados por el tenant actual (ver Fase 4).
 */
public interface MemberRepository extends JpaRepository<Member, UUID> {
}
