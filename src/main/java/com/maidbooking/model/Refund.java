package com.maidbooking.model;

import java.time.LocalDateTime;
import lombok.Value;

@Value
public class Refund {
    long amountPaise;
    String reference;
    LocalDateTime refundedAt;
}
