package com.gymflow.ai.infrastructure.template;

import com.gymflow.ai.application.GeneratedMessage;
import com.gymflow.ai.application.MessageGenerator;
import com.gymflow.ai.application.RiskAlertContext;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Component;

/**
 * Adapter sin costo ni API key — no hay proveedor de IA real conectado
 * todavía (ver package-info). "Personalización" acá significa variar el
 * tono según cuántos días pasaron, no un LLM generando texto libre; es
 * una base honesta para medir contra un proveedor real más adelante, no
 * una simulación de IA.
 */
@Component
public class TemplateMessageGenerator implements MessageGenerator {

    private static final long CONCERNING_THRESHOLD_DAYS = 14;

    @Override
    public GeneratedMessage generateRiskAlert(RiskAlertContext context) {
        long daysSinceLastActivity = ChronoUnit.DAYS.between(context.lastActivity(), LocalDate.now());

        if (daysSinceLastActivity >= CONCERNING_THRESHOLD_DAYS) {
            return new GeneratedMessage(
                    "¿Todo bien? Te extrañamos en el gimnasio",
                    "Hola " + context.firstName() + ", hace más de dos semanas que no te vemos. Si hay algo que te esté "
                            + "complicando venir — un cambio de horario, una duda sobre tu plan — contactanos, nos gustaría ayudarte a volver.");
        }

        return new GeneratedMessage(
                "Te extrañamos en el gimnasio",
                "Hola " + context.firstName() + ", notamos que no venís desde el " + context.lastActivity() + ". ¡Te esperamos pronto!");
    }
}
