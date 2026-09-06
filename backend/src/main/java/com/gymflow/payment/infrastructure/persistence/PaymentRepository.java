package com.gymflow.payment.infrastructure.persistence;

import com.gymflow.payment.domain.Payment;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    List<Payment> findByMembershipIdOrderByPaymentDateDesc(UUID membershipId);

    @Query("select coalesce(sum(p.amount), 0) from Payment p where p.paymentDate between :from and :to")
    BigDecimal sumAmountByPaymentDateBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
