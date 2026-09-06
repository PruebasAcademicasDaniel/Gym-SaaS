package com.gymflow.notification.application;

import com.gymflow.ai.application.GeneratedMessage;
import com.gymflow.ai.application.MessageGenerator;
import com.gymflow.ai.application.RiskAlertContext;
import com.gymflow.member.domain.Member;
import com.gymflow.membership.application.MembershipService;
import com.gymflow.membership.domain.Membership;
import com.gymflow.notification.domain.Notification;
import com.gymflow.notification.domain.NotificationType;
import com.gymflow.notification.infrastructure.persistence.NotificationRepository;
import com.gymflow.risk.application.AtRiskMember;
import com.gymflow.risk.application.RiskService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Depende de MembershipService/RiskService (no de sus repositorios) —
 * mismo patrón que Fases 7/8/10. Opera siempre sobre el tenant actual del
 * TenantContext; no recibe gymId ni lo necesita. El caller (el endpoint
 * manual o el scheduler cross-tenant) es quien decide para qué tenant
 * corre, seteando TenantContext antes de llamar acá.
 */
@Service
public class NotificationService {

    /** Mismo criterio que DashboardService (Fase 10) — a propósito: el mismo socio que ve "vence pronto" en el dashboard es al que le llega el recordatorio. */
    private static final int EXPIRING_SOON_WINDOW_DAYS = 7;

    private final NotificationRepository notificationRepository;
    private final MembershipService membershipService;
    private final RiskService riskService;
    private final EmailSender emailSender;
    private final MessageGenerator messageGenerator;

    public NotificationService(
            NotificationRepository notificationRepository, MembershipService membershipService, RiskService riskService,
            EmailSender emailSender, MessageGenerator messageGenerator) {
        this.notificationRepository = notificationRepository;
        this.membershipService = membershipService;
        this.riskService = riskService;
        this.emailSender = emailSender;
        this.messageGenerator = messageGenerator;
    }

    /** @return cuántos recordatorios se mandaron realmente (ya excluye los que no tenían email o ya habían sido notificados). */
    @Transactional
    public int sendExpirationReminders() {
        List<Membership> expiring = membershipService.listExpiringWithinDays(EXPIRING_SOON_WINDOW_DAYS);
        int sent = 0;

        for (Membership membership : expiring) {
            if (notificationRepository.existsByMembershipIdAndType(membership.getId(), NotificationType.MEMBERSHIP_EXPIRING_SOON)) {
                continue;
            }

            Member member = membership.getMember();
            if (member.getEmail() == null) {
                continue; // sin email no hay a quién mandarle nada — no es un error, solo un dato que falta
            }

            String subject = "Tu membresía vence pronto";
            String body = "Hola " + member.getFirstName() + ", tu membresía vence el " + membership.getEndDate() + ".";

            emailSender.send(member.getEmail(), subject, body);
            notificationRepository.save(new Notification(member, membership, NotificationType.MEMBERSHIP_EXPIRING_SOON, body));
            sent++;
        }

        return sent;
    }

    /**
     * Mismo criterio de idempotencia que sendExpirationReminders: una sola
     * alerta por membresía (no por "episodio de inactividad", que sería
     * difícil de acotar sin reintroducir estado propio). Si un socio sigue
     * en riesgo la próxima vez que corre esto, ya no se le vuelve a avisar
     * mientras dure esa misma membresía — evita spamear el mismo aviso día
     * tras día.
     *
     * El contenido del mensaje lo redacta MessageGenerator (Fase 15, capa
     * de IA sobre el motor de riesgo) — esta clase solo orquesta
     * idempotencia, envío y auditoría, igual que antes.
     */
    @Transactional
    public int sendRiskAlerts() {
        List<AtRiskMember> atRisk = riskService.listAtRiskMembers();
        int sent = 0;

        for (AtRiskMember candidate : atRisk) {
            if (notificationRepository.existsByMembershipIdAndType(candidate.membershipId(), NotificationType.MEMBER_AT_RISK)) {
                continue;
            }

            Member member = candidate.member();
            if (member.getEmail() == null) {
                continue; // sin email no hay a quién mandarle nada — no es un error, solo un dato que falta
            }

            GeneratedMessage message = messageGenerator.generateRiskAlert(new RiskAlertContext(member.getFirstName(), candidate.lastActivity()));

            emailSender.send(member.getEmail(), message.subject(), message.body());
            Membership membership = membershipService.getById(candidate.membershipId());
            notificationRepository.save(new Notification(member, membership, NotificationType.MEMBER_AT_RISK, message.body()));
            sent++;
        }

        return sent;
    }
}
