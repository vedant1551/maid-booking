package com.maidbooking.dto.request;

import com.maidbooking.model.PaymentMode;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PaymentRequest {
    @NotNull
    private PaymentMode mode;
}
