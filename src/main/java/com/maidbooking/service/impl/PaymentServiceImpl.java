package com.maidbooking.service.impl;

import com.maidbooking.exception.BadRequestException;
import com.maidbooking.exception.ConflictException;
import com.maidbooking.model.Payment;
import com.maidbooking.model.PaymentMode;
import com.maidbooking.model.Visit;
import com.maidbooking.payment.PaymentGateway;
import com.maidbooking.payment.PaymentGatewayFactory;
import com.maidbooking.repository.IdempotencyKeyRepository;
import com.maidbooking.service.BookingService;
import com.maidbooking.service.PaymentService;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private final BookingService bookingService;
    private final PaymentGatewayFactory paymentGatewayFactory;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final Clock clock;

    @Override
    public Payment payForVisit(UUID bookingId, UUID visitId, PaymentMode mode, String idempotencyKey) {
        BadRequestException.check(idempotencyKey != null && !idempotencyKey.isBlank(), "Idempotency-Key is required");
        PaymentGateway gateway = paymentGatewayFactory.get(mode);
        Visit visit = bookingService.getBooking(bookingId).findVisit(visitId);
        if (!idempotencyKeyRepository.claim(idempotencyKey, visitId).equals(visitId)) {
            throw new ConflictException("Idempotency-Key was already used for another visit");
        }
        return visit.pay(idempotencyKey, gateway, LocalDateTime.now(clock));
    }
}
