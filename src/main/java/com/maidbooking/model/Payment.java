package com.maidbooking.model;

import java.time.LocalDateTime;
import lombok.Value;

@Value
public class Payment {
    String idempotencyKey;
    PaymentMode mode;
    String reference;
    long amountPaise;
    LocalDateTime paidAt;

    public boolean isSameRequest(String key, PaymentMode requestedMode) {
        return idempotencyKey.equals(key) && mode == requestedMode;
    }
}
