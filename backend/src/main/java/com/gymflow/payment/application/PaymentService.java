package com.gymflow.payment.application;

import com.gymflow.audit.application.AuditService;
import com.gymflow.audit.domain.AuditAction;
import com.gymflow.membership.application.MembershipService;
import com.gymflow.membership.domain.Membership;
import com.gymflow.payment.domain.Payment;
import com.gymflow.payment.domain.PaymentMethod;
import com.gymflow.payment.infrastructure.persistence.PaymentRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Depende de MembershipService (no de MembershipRepository) por la misma
 * razón que MembershipService depende de MemberService/PlanService — ver
 * ese comentario. Los pagos son parte de la auditoría mínima del MVP
 * (Fase 0, sección 12: "login, pagos, altas y bajas de socio").
 */
@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final MembershipService membershipService;
    private final AuditService auditService;

    public PaymentService(PaymentRepository paymentRepository, MembershipService membershipService, AuditService auditService) {
        this.paymentRepository = paymentRepository;
        this.membershipService = membershipService;
        this.auditService = auditService;
    }

    @Transactional
    public Payment register(UUID actorUserId, UUID membershipId, BigDecimal amount, PaymentMethod method) {
        Membership membership = membershipService.getById(membershipId);
        Payment payment = paymentRepository.save(new Payment(membership, amount, method, LocalDate.now()));
        auditService.record(payment.getGymId(), actorUserId, AuditAction.PAYMENT_REGISTERED, payment.getId().toString());
        return payment;
    }

    public Payment getById(UUID id) {
        return paymentRepository.findById(id).orElseThrow(() -> new PaymentNotFoundException(id));
    }

    public List<Payment> listByMembership(UUID membershipId) {
        membershipService.getById(membershipId); // 404 temprano si la membresía no existe o es de otro gimnasio
        return paymentRepository.findByMembershipIdOrderByPaymentDateDesc(membershipId);
    }

    /** Suma de pagos del mes calendario en curso (desde el día 1 hasta hoy) — puerta pública para dashboard. */
    public BigDecimal sumRevenueForCurrentMonth() {
        LocalDate today = LocalDate.now();
        return paymentRepository.sumAmountByPaymentDateBetween(today.withDayOfMonth(1), today);
    }
}
