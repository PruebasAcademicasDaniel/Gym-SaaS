package com.gymflow.notification.application;

import com.gymflow.member.domain.Member;
import com.gymflow.membership.application.MembershipService;
import com.gymflow.membership.domain.Membership;
import com.gymflow.notification.domain.Notification;
import com.gymflow.notification.domain.NotificationType;
import com.gymflow.notification.infrastructure.persistence.NotificationRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Depende de MembershipService (no de MembershipRepository) — mismo
 * patrón que Fases 7/8/10. Opera siempre sobre el tenant actual del
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
    private final EmailSender emailSender;

    public NotificationService(
            NotificationRepository notificationRepository, MembershipService membershipService, EmailSender emailSender) {
        this.notificationRepository = notificationRepository;
        this.membershipService = membershipService;
        this.emailSender = emailSender;
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
}
