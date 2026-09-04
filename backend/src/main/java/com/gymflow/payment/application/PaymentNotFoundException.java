package com.gymflow.payment.application;

import com.gymflow.shared.error.NotFoundException;
import java.util.UUID;

public class PaymentNotFoundException extends NotFoundException {

    public PaymentNotFoundException(UUID id) {
        super("No existe un pago con id " + id + ".");
    }
}
