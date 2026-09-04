package com.gymflow.membership.infrastructure.persistence;

import com.gymflow.membership.domain.Membership;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipRepository extends JpaRepository<Membership, UUID> {

    /** Sigue filtrado por tenant igual que findAll()/findById() — Membership también extiende AbstractTenantEntity. */
    List<Membership> findByMemberIdOrderByStartDateDesc(UUID memberId);
}
