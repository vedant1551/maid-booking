package com.maidbooking.payment;

import com.maidbooking.model.PaymentMode;
import org.springframework.stereotype.Component;

@Component
public class WalletPaymentGateway extends MockPaymentGateway {

    @Override
    public PaymentMode getMode() {
        return PaymentMode.WALLET;
    }
}
