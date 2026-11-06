package com.maidbooking.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RefundResponse {
    @Schema(description = "Rupees", example = "240.50")
    private final BigDecimal amount;
    private final String reference;
    private final LocalDateTime refundedAt;
}
