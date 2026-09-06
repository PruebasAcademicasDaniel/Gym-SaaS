package com.gymflow.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.gymflow.ai.application.GeneratedMessage;
import com.gymflow.ai.application.RiskAlertContext;
import com.gymflow.ai.infrastructure.template.TemplateMessageGenerator;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** Unit puro, sin contexto de Spring — mismo criterio que RiskPolicyTest. */
class TemplateMessageGeneratorTest {

    private final TemplateMessageGenerator generator = new TemplateMessageGenerator();

    @Test
    void recentInactivity_usesTheStandardTone() {
        GeneratedMessage message = generator.generateRiskAlert(new RiskAlertContext("Carlos", LocalDate.now().minusDays(6)));

        assertThat(message.subject()).isEqualTo("Te extrañamos en el gimnasio");
        assertThat(message.body()).contains("Carlos");
    }

    @Test
    void longInactivity_usesTheMoreConcernedTone() {
        GeneratedMessage message = generator.generateRiskAlert(new RiskAlertContext("Ana", LocalDate.now().minusDays(20)));

        assertThat(message.subject()).isEqualTo("¿Todo bien? Te extrañamos en el gimnasio");
        assertThat(message.body()).contains("Ana").contains("dos semanas");
    }
}
