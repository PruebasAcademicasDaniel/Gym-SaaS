package com.gymflow.risk;

import static org.assertj.core.api.Assertions.assertThat;

import com.gymflow.risk.domain.RiskPolicy;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** Unit puro, sin contexto de Spring — la regla de negocio del motor de riesgo no debería necesitarlo (Fase 0, sección de testing). */
class RiskPolicyTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 10);

    @Test
    void activityWithinTheThreshold_isNotAtRisk() {
        LocalDate lastActivity = TODAY.minusDays(RiskPolicy.INACTIVITY_THRESHOLD_DAYS);

        assertThat(RiskPolicy.isAtRisk(lastActivity, TODAY)).isFalse();
    }

    @Test
    void activityOneDayPastTheThreshold_isAtRisk() {
        LocalDate lastActivity = TODAY.minusDays(RiskPolicy.INACTIVITY_THRESHOLD_DAYS + 1);

        assertThat(RiskPolicy.isAtRisk(lastActivity, TODAY)).isTrue();
    }

    @Test
    void activityToday_isNeverAtRisk() {
        assertThat(RiskPolicy.isAtRisk(TODAY, TODAY)).isFalse();
    }

    @Test
    void wellPastTheThreshold_isAtRisk() {
        assertThat(RiskPolicy.isAtRisk(TODAY.minusDays(30), TODAY)).isTrue();
    }
}
