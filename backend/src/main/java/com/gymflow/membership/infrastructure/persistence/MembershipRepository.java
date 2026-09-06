package com.gymflow.membership.infrastructure.persistence;

import com.gymflow.membership.domain.Membership;
import com.gymflow.membership.domain.MembershipStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MembershipRepository extends JpaRepository<Membership, UUID> {

    /** Sigue filtrado por tenant igual que findAll()/findById() — Membership también extiende AbstractTenantEntity. */
    List<Membership> findByMemberIdOrderByStartDateDesc(UUID memberId);

    /**
     * Socios distintos con una membresía vigente (ACTIVE y no vencida
     * todavía). Las consultas JPQL contra una entidad @TenantId siguen
     * filtradas por tenant a nivel de Hibernate — no es exclusivo de los
     * métodos derivados por nombre (findAll/findById).
     */
    @Query("select count(distinct m.member.id) from Membership m where m.status = :status and m.endDate >= :today")
    long countDistinctMembersWithStatusAndNotExpired(@Param("status") MembershipStatus status, @Param("today") LocalDate today);

    @Query("select count(m) from Membership m where m.status = :status and m.endDate between :from and :to")
    long countByStatusAndEndDateBetween(
            @Param("status") MembershipStatus status, @Param("from") LocalDate from, @Param("to") LocalDate to);

    /** Mismo criterio que countByStatusAndEndDateBetween, pero trayendo las filas — para el módulo notification (Fase 12). */
    List<Membership> findByStatusAndEndDateBetween(MembershipStatus status, LocalDate from, LocalDate to);
}
