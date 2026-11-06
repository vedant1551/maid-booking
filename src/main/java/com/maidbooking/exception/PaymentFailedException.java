package com.maidbooking.exception;

import org.springframework.http.HttpStatus;

public class PaymentFailedException extends ApiException {

    public PaymentFailedException(String message) {
        super(message);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.BAD_GATEWAY;
    }
}
