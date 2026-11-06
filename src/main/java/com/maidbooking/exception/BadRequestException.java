package com.maidbooking.exception;

import org.springframework.http.HttpStatus;

public class BadRequestException extends ApiException {

    public BadRequestException(String message) {
        super(message);
    }

    public static void check(boolean condition, String message) {
        if (!condition) {
            throw new BadRequestException(message);
        }
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.BAD_REQUEST;
    }
}
