package com.maidbooking.refund;

import com.maidbooking.model.Payment;
import com.maidbooking.model.TimeSlot;
import java.time.LocalDateTime;

public interface RefundPolicy {

    long calculateRefund(Payment payment, TimeSlot slot, LocalDateTime cancelledAt);
}
