package com.maidbooking.model;

import com.maidbooking.exception.ConflictException;
import com.maidbooking.payment.PaymentGateway;
import com.maidbooking.payment.PaymentGatewayFactory;
import com.maidbooking.refund.RefundPolicy;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;

/** Pay and cancel on the same visit never run at the same time. */
public class Visit {
    @Getter
    private final UUID id = UUID.randomUUID();
    @Getter
    private final TimeSlot slot;
    @Getter
    private final long pricePaise;

    private VisitStatus status = VisitStatus.BOOKED;
    private Payment payment;
    private Refund refund;
    private Integer rating;

    public Visit(TimeSlot slot, long pricePaise) {
        if (pricePaise < 0) {
            throw new IllegalStateException("Price must not be negative");
        }
        this.slot = slot;
        this.pricePaise = pricePaise;
    }

    public synchronized Payment pay(String idempotencyKey, PaymentGateway gateway, LocalDateTime now) {
        if (payment != null && payment.isSameRequest(idempotencyKey, gateway.getMode())) {
            return payment;
        }
        status.checkPayable();
        String reference = gateway.charge(id, pricePaise, idempotencyKey);
        payment = new Payment(idempotencyKey, gateway.getMode(), reference, pricePaise, now);
        status = VisitStatus.PAID;
        return payment;
    }

    public void cancel(LocalDateTime now, RefundPolicy refundPolicy, PaymentGatewayFactory gateways) {
        if (!cancelIfActive(now, refundPolicy, gateways)) {
            throw new ConflictException("Visit is already cancelled or has already started");
        }
    }

    public synchronized boolean cancelIfActive(LocalDateTime now, RefundPolicy refundPolicy,
                                               PaymentGatewayFactory gateways) {
        if (!status.isCancellable() || hasStarted(now)) {
            return false;
        }
        if (payment != null) {
            long amount = refundPolicy.calculateRefund(payment, slot, now);
            if (amount < 0 || amount > payment.getAmountPaise()) {
                throw new IllegalStateException("Refund must be between zero and the paid amount");
            }
            String reference = amount > 0 ? gateways.get(payment.getMode()).refund(payment.getReference(), amount) : null;
            refund = new Refund(amount, reference, now);
        }
        status = VisitStatus.CANCELLED;
        return true;
    }

    public synchronized void rate(int stars, LocalDateTime now) {
        if (status == VisitStatus.CANCELLED) {
            throw new ConflictException("A cancelled visit cannot be rated");
        }
        if (!hasEnded(now)) {
            throw new ConflictException("A visit can be rated only after it ends");
        }
        if (rating != null) {
            throw new ConflictException("Visit is already rated");
        }
        rating = stars;
    }

    public synchronized Integer getRating() {
        return rating;
    }

    public synchronized VisitStatus getStatus() {
        return status;
    }

    public synchronized Payment getPayment() {
        return payment;
    }

    public synchronized Refund getRefund() {
        return refund;
    }

    private boolean hasStarted(LocalDateTime now) {
        return !now.isBefore(slot.getStart());
    }

    private boolean hasEnded(LocalDateTime now) {
        return !now.isBefore(slot.getEnd());
    }
}
