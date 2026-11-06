package com.maidbooking.payment;

import com.maidbooking.model.PaymentMode;
import org.springframework.stereotype.Component;

@Component
public class CardPaymentGateway extends MockPaymentGateway {

    @Override
    public PaymentMode getMode() {
        return PaymentMode.CARD;
    }
}
