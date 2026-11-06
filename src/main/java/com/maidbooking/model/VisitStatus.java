package com.maidbooking.model;

import com.maidbooking.exception.ConflictException;

public enum VisitStatus {
    BOOKED(true, true),
    PAID(false, true),
    CANCELLED(false, false);

    private final boolean payable;
    private final boolean cancellable;

    VisitStatus(boolean payable, boolean cancellable) {
        this.payable = payable;
        this.cancellable = cancellable;
    }

    public boolean isCancellable() {
        return cancellable;
    }

    public void checkPayable() {
        if (!payable) {
            throw new ConflictException("Visit is already " + name().toLowerCase());
        }
    }
}
