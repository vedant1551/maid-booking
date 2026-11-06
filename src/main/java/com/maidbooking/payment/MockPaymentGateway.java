package com.maidbooking.payment;

import java.util.UUID;

public abstract class MockPaymentGateway implements PaymentGateway {

    @Override
    public final String charge(UUID visitId, long amountPaise, String idempotencyKey) {
        return referencePrefix() + "_ch_" + UUID.randomUUID();
    }

    @Override
    public final String refund(String chargeReference, long amountPaise) {
        return referencePrefix() + "_rf_" + UUID.randomUUID();
    }

    private String referencePrefix() {
        return getMode().name().toLowerCase();
    }
}
