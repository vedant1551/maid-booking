package com.maidbooking.pricing;

import com.maidbooking.model.Maid;
import com.maidbooking.model.ServiceType;
import com.maidbooking.model.TimeSlot;

public interface PricingStrategy {

    long calculatePrice(Maid maid, ServiceType service, TimeSlot slot);
}
