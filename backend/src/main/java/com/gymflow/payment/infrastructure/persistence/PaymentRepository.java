package com.gymflow.payment.infrastructure.persistence;

import com.gymflow.payment.domain.Payment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    List<Payment> findByMembershipIdOrderByPaymentDateDesc(UUID membershipId);
}
