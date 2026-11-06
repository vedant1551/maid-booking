package com.maidbooking.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import com.maidbooking.model.VisitStatus;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class VisitResponse {
    private final UUID id;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    @Schema(description = "Rupees", example = "240.50")
    private final BigDecimal price;
    private final VisitStatus status;
    private final PaymentResponse payment;
    private final RefundResponse refund;
    private final Integer rating;
}
