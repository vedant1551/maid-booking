package com.maidbooking.pricing;

import com.maidbooking.model.Maid;
import com.maidbooking.model.ServiceType;
import com.maidbooking.model.TimeSlot;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class HourlyPricingStrategy implements PricingStrategy {
    private static final BigDecimal MINUTES_PER_HOUR = BigDecimal.valueOf(60);

    @Override
    public long calculatePrice(Maid maid, ServiceType service, TimeSlot slot) {
        BigDecimal minutes = BigDecimal.valueOf(slot.getDuration().toMinutes());
        return BigDecimal.valueOf(maid.getHourlyRatePaise())
                .multiply(minutes)
                .divide(MINUTES_PER_HOUR, 0, RoundingMode.HALF_UP)
                .longValueExact();
    }
}
