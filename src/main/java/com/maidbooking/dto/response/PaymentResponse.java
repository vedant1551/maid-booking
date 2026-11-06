package com.maidbooking.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import com.maidbooking.model.PaymentMode;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PaymentResponse {
    private final String idempotencyKey;
    private final PaymentMode mode;
    private final String reference;
    @Schema(description = "Rupees", example = "240.50")
    private final BigDecimal amount;
    private final LocalDateTime paidAt;
}
