package com.maidbooking.refund;

import com.maidbooking.model.Payment;
import com.maidbooking.model.TimeSlot;
import java.time.Duration;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

@Component
public class TieredRefundPolicy implements RefundPolicy {
    private static final Duration FULL_REFUND_NOTICE = Duration.ofHours(24);
    private static final Duration HALF_REFUND_NOTICE = Duration.ofHours(2);

    @Override
    public long calculateRefund(Payment payment, TimeSlot slot, LocalDateTime cancelledAt) {
        Duration notice = Duration.between(cancelledAt, slot.getStart());
        if (notice.compareTo(FULL_REFUND_NOTICE) >= 0) {
            return payment.getAmountPaise();
        }
        if (notice.compareTo(HALF_REFUND_NOTICE) >= 0) {
            return payment.getAmountPaise() / 2;
        }
        return 0;
    }
}
