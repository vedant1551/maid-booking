package com.maidbooking.payment;

import com.maidbooking.model.PaymentMode;
import org.springframework.stereotype.Component;

@Component
public class UpiPaymentGateway extends MockPaymentGateway {

    @Override
    public PaymentMode getMode() {
        return PaymentMode.UPI;
    }
}
