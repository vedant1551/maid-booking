package com.maidbooking.payment;

import com.maidbooking.model.PaymentMode;
import java.util.UUID;

public interface PaymentGateway {

    PaymentMode getMode();

    String charge(UUID visitId, long amountPaise, String idempotencyKey);

    String refund(String chargeReference, long amountPaise);
}
