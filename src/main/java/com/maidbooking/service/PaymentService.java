package com.maidbooking.service;

import com.maidbooking.model.Payment;
import com.maidbooking.model.PaymentMode;
import java.util.UUID;

public interface PaymentService {

    Payment payForVisit(UUID bookingId, UUID visitId, PaymentMode mode, String idempotencyKey);
}
