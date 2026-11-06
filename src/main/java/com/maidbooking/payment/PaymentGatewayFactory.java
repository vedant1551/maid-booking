package com.maidbooking.payment;

import com.maidbooking.exception.BadRequestException;
import com.maidbooking.model.PaymentMode;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class PaymentGatewayFactory {
    private final Map<PaymentMode, PaymentGateway> gateways = new EnumMap<>(PaymentMode.class);

    public PaymentGatewayFactory(List<PaymentGateway> gateways) {
        gateways.forEach(gateway -> this.gateways.put(gateway.getMode(), gateway));
    }

    public PaymentGateway get(PaymentMode mode) {
        PaymentGateway gateway = mode == null ? null : gateways.get(mode);
        BadRequestException.check(gateway != null, "Unsupported payment mode: " + mode);
        return gateway;
    }
}
