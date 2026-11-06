package com.maidbooking.dto.money;

import com.maidbooking.exception.BadRequestException;
import java.math.BigDecimal;
import lombok.experimental.UtilityClass;

@UtilityClass
public class Rupees {
    private static final int PAISE_SCALE = 2;

    public long toPaise(BigDecimal rupees) {
        BadRequestException.check(rupees.scale() <= PAISE_SCALE, "Amount can have at most 2 decimal places");
        return rupees.movePointRight(PAISE_SCALE).longValueExact();
    }

    public BigDecimal fromPaise(long paise) {
        return BigDecimal.valueOf(paise, PAISE_SCALE);
    }
}
