package com.gymflow.membership;

import static org.assertj.core.api.Assertions.assertThat;

import com.gymflow.member.domain.Member;
import com.gymflow.membership.domain.Membership;
import com.gymflow.membership.domain.MembershipStatus;
import com.gymflow.plan.domain.Plan;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/**
 * Unit puro, sin Spring ni base de datos — el ciclo de vida de Membership
 * es justo el tipo de regla de negocio que la Fase 0 pedía cubrir así
 * (sección 13: "JUnit 5 puro... ciclo de vida de Membership").
 */
class MembershipTest {

    @Test
    void endDateIsComputedFromStartDatePlusThePlansDuration() {
        LocalDate start = LocalDate.now();
        Membership membership = new Membership(aMember(), aPlan(45), start);

        assertThat(membership.getEndDate()).isEqualTo(start.plusDays(45));
    }

    @Test
    void isActiveWhileEndDateIsInTheFuture() {
        Membership membership = new Membership(aMember(), aPlan(30), LocalDate.now());

        assertThat(membership.getEffectiveStatus()).isEqualTo(MembershipStatus.ACTIVE);
    }

    @Test
    void isExpiredOnceEndDateHasPassed_evenThoughNoJobEverTouchedIt() {
        // arrancó hace 10 días con un plan de 5 — venció hace 5 días, y nadie la marcó nunca.
        Membership membership = new Membership(aMember(), aPlan(5), LocalDate.now().minusDays(10));

        assertThat(membership.getEffectiveStatus()).isEqualTo(MembershipStatus.EXPIRED);
    }

    @Test
    void cancelledWinsEvenIfStillWithinItsDateRange() {
        Membership membership = new Membership(aMember(), aPlan(30), LocalDate.now());

        membership.cancel();

        assertThat(membership.getEffectiveStatus()).isEqualTo(MembershipStatus.CANCELLED);
    }

    private Member aMember() {
        return new Member("Ana", "Gómez", null, null);
    }

    private Plan aPlan(int durationDays) {
        return new Plan("Mensual", null, new BigDecimal("100.00"), durationDays);
    }
}
